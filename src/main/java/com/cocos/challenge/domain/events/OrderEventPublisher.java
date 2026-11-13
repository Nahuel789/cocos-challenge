package com.cocos.challenge.domain.events;

import com.cocos.challenge.domain.model.Order;

public interface OrderEventPublisher {

	void publish(Order order);
}
