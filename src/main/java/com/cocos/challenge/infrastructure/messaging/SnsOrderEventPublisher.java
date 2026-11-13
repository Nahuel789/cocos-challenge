package com.cocos.challenge.infrastructure.messaging;

import com.cocos.challenge.domain.events.OrderEventPublisher;
import com.cocos.challenge.domain.model.Order;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import tools.jackson.databind.json.JsonMapper;

@Log4j2
public final class SnsOrderEventPublisher implements OrderEventPublisher {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final SnsClient sns;
	private final String topicArn;

	public SnsOrderEventPublisher(SnsClient sns, String topicName) {
		this.sns = sns;
		this.topicArn = sns.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
		log.info("SNS topic ready name={} arn={}", topicName, topicArn);
	}

	@Override
	public void publish(Order order) {
		try {
			String body = JSON.writeValueAsString(new OrderPersistedMessage(
					"ORDER_PERSISTED",
					order.id(),
					order.userId(),
					order.instrumentId(),
					order.side().name(),
					order.status().name()));
			sns.publish(PublishRequest.builder().topicArn(topicArn).message(body).build());
			log.info("Published ORDER_PERSISTED orderId={}, userId={}, status={}",
					order.id(), order.userId(), order.status());
		}
		catch (Exception ex) {
			log.error("Failed to publish ORDER_PERSISTED orderId={}, userId={}: {}",
					order.id(), order.userId(), ex.getMessage(), ex);
		}
	}

	private record OrderPersistedMessage(
			String type,
			Long orderId,
			Long userId,
			Long instrumentId,
			String side,
			String status
	) {
	}
}
