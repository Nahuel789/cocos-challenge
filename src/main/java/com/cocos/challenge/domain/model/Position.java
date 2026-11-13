package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public record Position(
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

	public static Position of(Instrument instrument, Lot lot, Quote quote) {
		BigDecimal close = quote == null ? null : quote.close();
		BigDecimal marketPrice = close == null ? Decimals.money(BigDecimal.ZERO) : Decimals.price(close);
		return new Position(
				instrument.id(),
				instrument.ticker(),
				instrument.name(),
				Decimals.quantity(lot.quantity()),
				marketPrice,
				Decimals.money(lot.quantity().multiply(marketPrice)),
				lot.averagePrice(),
				lot.totalReturnPercent(close),
				quote == null ? null : quote.dailyReturnPercent());
	}
}
