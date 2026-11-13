package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Decimals {

	public static final int MONEY_SCALE = 2;
	public static final int PRICE_SCALE = 4;
	public static final int PERCENT_SCALE = 4;
	public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	private Decimals() {
	}

	public static BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE, ROUNDING);
	}

	public static BigDecimal price(BigDecimal value) {
		return value.setScale(PRICE_SCALE, ROUNDING);
	}

	public static BigDecimal percent(BigDecimal value) {
		return value.setScale(PERCENT_SCALE, ROUNDING);
	}

	public static BigDecimal quantity(BigDecimal value) {
		BigDecimal stripped = value.stripTrailingZeros();
		if (stripped.scale() < 0) {
			return stripped.setScale(0, ROUNDING);
		}
		return stripped;
	}

	public static boolean isWhole(BigDecimal value) {
		return value.stripTrailingZeros().scale() <= 0;
	}
}
