package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
		Long userId,
		String accountNumber,
		BigDecimal totalValue,
		BigDecimal availableCash,
		List<Position> positions
) {
}
