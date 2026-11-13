package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Position;

import java.math.BigDecimal;

public record PositionResponse(
		Long instrumentId,
		String ticker,
		String name,
		BigDecimal quantity,
		BigDecimal marketPrice,
		BigDecimal marketValue,
		BigDecimal averagePrice,
		BigDecimal totalReturnPercent,
		BigDecimal dailyReturnPercent
) {

	public static PositionResponse from(Position position) {
		return new PositionResponse(
				position.instrumentId(),
				position.ticker(),
				position.name(),
				position.quantity(),
				position.marketPrice(),
				position.marketValue(),
				position.averagePrice(),
				position.totalReturnPercent(),
				position.dailyReturnPercent());
	}
}
