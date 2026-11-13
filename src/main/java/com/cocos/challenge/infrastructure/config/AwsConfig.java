package com.cocos.challenge.infrastructure.config;

import com.cocos.challenge.domain.events.OrderEventPublisher;
import com.cocos.challenge.infrastructure.messaging.NoOpOrderEventPublisher;
import com.cocos.challenge.infrastructure.messaging.SnsOrderEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

	@Bean(destroyMethod = "close")
	@ConditionalOnProperty(prefix = "aws.sns", name = "enabled", havingValue = "true")
	SnsClient snsClient(@Value("${aws.region}") String region, @Value("${aws.sns.endpoint}") String endpoint) {
		return SnsClient.builder()
				.region(Region.of(region))
				.endpointOverride(URI.create(endpoint))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
				.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "aws.sns", name = "enabled", havingValue = "true")
	OrderEventPublisher snsOrderEventPublisher(SnsClient sns, @Value("${aws.sns.topic-name}") String topicName) {
		return new SnsOrderEventPublisher(sns, topicName);
	}

	@Bean
	@ConditionalOnProperty(prefix = "aws.sns", name = "enabled", havingValue = "false")
	OrderEventPublisher noOpOrderEventPublisher() {
		return new NoOpOrderEventPublisher();
	}
}
