package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record Holdings(
		BigDecimal settledCash,
		BigDecimal availableCash,
		BigDecimal totalValue,
		Map<Long, BigDecimal> availableShares,
		List<Position> positions
) {
}
