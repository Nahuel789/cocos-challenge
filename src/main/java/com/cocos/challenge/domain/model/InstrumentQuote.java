package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public record InstrumentQuote(
		Long id,
		String ticker,
		String name,
		InstrumentType type,
		BigDecimal lastPrice,
		BigDecimal dailyReturnPercent
) {
}
