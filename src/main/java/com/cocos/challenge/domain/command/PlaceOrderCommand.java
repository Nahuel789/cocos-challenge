package com.cocos.challenge.domain.command;

import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderType;

import java.math.BigDecimal;

public record PlaceOrderCommand(
		Long userId,
		Long instrumentId,
		OrderSide side,
		OrderType type,
		BigDecimal quantity,
		BigDecimal amount,
		BigDecimal price
) {
}
