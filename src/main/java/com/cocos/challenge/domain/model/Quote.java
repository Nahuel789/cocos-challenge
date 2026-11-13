package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public record Quote(Long instrumentId, BigDecimal close, BigDecimal previousClose) {

	public BigDecimal dailyReturnPercent() {
		if (close == null || previousClose == null || previousClose.signum() == 0) {
			return null;
		}
		return Decimals.percent(close.subtract(previousClose)
				.divide(previousClose, 8, Decimals.ROUNDING)
				.multiply(BigDecimal.valueOf(100)));
	}
}
