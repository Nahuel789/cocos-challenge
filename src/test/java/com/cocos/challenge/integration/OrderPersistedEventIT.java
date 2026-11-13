package com.cocos.challenge.integration;

import com.cocos.challenge.config.PostgresTestcontainersConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfig.class)
@Sql(scripts = "/sql/orders-fixture.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderPersistedEventIT {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private static final JsonMapper JSON = JsonMapper.builder().build();

	static final GenericContainer<?> LOCALSTACK = new GenericContainer<>("localstack/localstack:4")
			.withExposedPorts(4566)
			.withEnv("SERVICES", "sns,sqs")
			.withEnv("SQS_ENDPOINT_STRATEGY", "path")
			.withStartupTimeout(Duration.ofMinutes(4))
			.waitingFor(Wait.forHttp("/_localstack/health").forPort(4566).forStatusCode(200));

	static {
		LOCALSTACK.start();
	}

	@DynamicPropertySource
	static void localstack(DynamicPropertyRegistry registry) {
		registry.add("aws.sns.enabled", () -> "true");
		registry.add("aws.sns.endpoint", OrderPersistedEventIT::endpoint);
	}

	private static SnsClient sns;
	private static SqsClient sqs;

	@Autowired
	private MockMvc mockMvc;

	@BeforeAll
	static void clients() {
		sns = client(SnsClient.builder()).build();
		sqs = client(SqsClient.builder()).build();
	}

	@AfterAll
	static void closeClients() {
		if (sns != null) {
			sns.close();
		}
		if (sqs != null) {
			sqs.close();
		}
	}

	@Test
	void persistedOrderPublishesEventSoClientCanRefreshPortfolio() throws Exception {
		String queueUrl = subscribe();

		MvcResult result = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		Number orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		JsonNode event = awaitMessage(queueUrl);
		assertEquals("ORDER_PERSISTED", event.get("type").stringValue());
		assertEquals(10L, event.get("userId").asLong());
		assertEquals(orderId.longValue(), event.get("orderId").asLong());
		assertEquals("FILLED", event.get("status").stringValue());
	}

	@Test
	void cancelledOrderPublishesEventSoClientCanRefreshPortfolio() throws Exception {
		MvcResult placed = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"LIMIT","quantity":1,"price":100}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		Number orderId = JsonPath.read(placed.getResponse().getContentAsString(), "$.id");

		String queueUrl = subscribe();
		mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId.longValue()))
				.andExpect(status().isOk());

		JsonNode event = awaitMessage(queueUrl);
		assertEquals("ORDER_PERSISTED", event.get("type").stringValue());
		assertEquals(10L, event.get("userId").asLong());
		assertEquals(orderId.longValue(), event.get("orderId").asLong());
		assertEquals("CANCELLED", event.get("status").stringValue());
	}

	private static String subscribe() {
		String queueName = "order-persisted-" + System.nanoTime();
		String created = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
		String queueUrl = reachable(created, queueName);
		String queueArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
						.queueUrl(queueUrl)
						.attributeNames(QueueAttributeName.QUEUE_ARN)
						.build())
				.attributes()
				.get(QueueAttributeName.QUEUE_ARN);
		sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
				.queueUrl(queueUrl)
				.attributes(Map.of(QueueAttributeName.POLICY, """
						{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":"sqs:SendMessage","Resource":"%s"}]}
						""".formatted(queueArn)))
				.build());
		String topicArn = sns.listTopics().topics().stream()
				.map(Topic::topicArn)
				.filter(arn -> arn.endsWith(":order-persisted"))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("topic order-persisted was not created"));
		sns.subscribe(SubscribeRequest.builder()
				.topicArn(topicArn)
				.protocol("sqs")
				.endpoint(queueArn)
				.attributes(Map.of("RawMessageDelivery", "true"))
				.build());
		return queueUrl;
	}

	private static JsonNode awaitMessage(String queueUrl) throws Exception {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueUrl)
					.maxNumberOfMessages(5)
					.waitTimeSeconds(2)
					.build()).messages();
			if (!messages.isEmpty()) {
				return payload(messages.get(0).body());
			}
		}
		return fail("no ORDER_PERSISTED message on " + queueUrl);
	}

	private static JsonNode payload(String body) throws Exception {
		JsonNode node = JSON.readTree(body);
		JsonNode embedded = node.get("Message");
		if (embedded != null && embedded.isString()) {
			return JSON.readTree(embedded.stringValue());
		}
		return node;
	}

	private static String reachable(String queueUrl, String queueName) {
		if (queueUrl.startsWith(endpoint())) {
			return queueUrl;
		}
		return endpoint() + "/000000000000/" + queueName;
	}

	private static String endpoint() {
		return "http://localhost:" + LOCALSTACK.getMappedPort(4566);
	}

	private static <B extends software.amazon.awssdk.awscore.client.builder.AwsClientBuilder<B, ?>> B client(B builder) {
		return builder.region(Region.US_EAST_1)
				.endpointOverride(URI.create(endpoint()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
	}
}
