package com.cocos.challenge.domain.model;

import java.math.BigDecimal;

public final class Lot {

	private BigDecimal quantity = BigDecimal.ZERO;
	private BigDecimal cost = BigDecimal.ZERO;
	private BigDecimal reserved = BigDecimal.ZERO;

	public void buy(BigDecimal size, BigDecimal price) {
		quantity = quantity.add(size);
		cost = cost.add(size.multiply(price));
	}

	public void sell(BigDecimal size) {
		if (quantity.signum() > 0) {
			BigDecimal average = cost.divide(quantity, 8, Decimals.ROUNDING);
			cost = cost.subtract(average.multiply(size)).max(BigDecimal.ZERO);
		}
		quantity = quantity.subtract(size);
	}

	public void reserve(BigDecimal size) {
		reserved = reserved.add(size);
	}

	public BigDecimal available() {
		return Decimals.quantity(quantity.subtract(reserved));
	}

	public boolean isOpen() {
		return quantity.signum() > 0;
	}

	public BigDecimal quantity() {
		return quantity;
	}

	public BigDecimal averagePrice() {
		return Decimals.price(cost.divide(quantity, 8, Decimals.ROUNDING));
	}

	public BigDecimal totalReturnPercent(BigDecimal close) {
		BigDecimal averagePrice = averagePrice();
		if (averagePrice.signum() == 0 || close == null) {
			return null;
		}
		return Decimals.percent(close.subtract(averagePrice)
				.divide(averagePrice, 8, Decimals.ROUNDING)
				.multiply(BigDecimal.valueOf(100)));
	}
}
