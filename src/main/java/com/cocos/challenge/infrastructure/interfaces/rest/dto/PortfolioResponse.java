package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
		Long userId,
		String accountNumber,
		BigDecimal totalValue,
		BigDecimal availableCash,
		List<PositionResponse> positions
) {

	public static PortfolioResponse from(Portfolio portfolio) {
		return new PortfolioResponse(
				portfolio.userId(),
				portfolio.accountNumber(),
				portfolio.totalValue(),
				portfolio.availableCash(),
				portfolio.positions().stream().map(PositionResponse::from).toList());
	}
}
