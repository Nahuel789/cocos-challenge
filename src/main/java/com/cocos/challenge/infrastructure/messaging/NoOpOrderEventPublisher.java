package com.cocos.challenge.infrastructure.messaging;

import com.cocos.challenge.domain.events.OrderEventPublisher;
import com.cocos.challenge.domain.model.Order;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class NoOpOrderEventPublisher implements OrderEventPublisher {

	@Override
	public void publish(Order order) {
		log.debug("SNS disabled; skipped ORDER_PERSISTED orderId={}, userId={}", order.id(), order.userId());
	}
}
