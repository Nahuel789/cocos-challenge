package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.InstrumentQuote;
import com.cocos.challenge.domain.model.InstrumentType;

import java.math.BigDecimal;

public record InstrumentResponse(
		Long id,
		String ticker,
		String name,
		InstrumentType type,
		BigDecimal lastPrice,
		BigDecimal dailyReturnPercent
) {

	public static InstrumentResponse from(InstrumentQuote quote) {
		return new InstrumentResponse(
				quote.id(),
				quote.ticker(),
				quote.name(),
				quote.type(),
				quote.lastPrice(),
				quote.dailyReturnPercent());
	}
}
