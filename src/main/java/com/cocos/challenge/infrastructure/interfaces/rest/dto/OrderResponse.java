package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderStatus;
import com.cocos.challenge.domain.model.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
		Long id,
		Long userId,
		Long instrumentId,
		OrderSide side,
		OrderType type,
		BigDecimal size,
		BigDecimal price,
		OrderStatus status,
		Instant datetime
) {

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.id(),
				order.userId(),
				order.instrumentId(),
				order.side(),
				order.type(),
				order.size(),
				order.price(),
				order.status(),
				order.datetime());
	}
}
