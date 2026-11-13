package com.cocos.challenge.application.usecases;

import com.cocos.challenge.application.service.HoldingCalculator;
import com.cocos.challenge.domain.exception.UserNotFoundException;
import com.cocos.challenge.domain.model.*;
import com.cocos.challenge.domain.repositories.InstrumentRepository;
import com.cocos.challenge.domain.repositories.MarketDataRepository;
import com.cocos.challenge.domain.repositories.OrderRepository;
import com.cocos.challenge.domain.repositories.UserRepository;
import com.cocos.challenge.domain.usecases.GetPortfolioUseCase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GetPortfolioUseCaseImpl implements GetPortfolioUseCase {

	private final UserRepository users;
	private final OrderRepository orders;
	private final InstrumentRepository instruments;
	private final MarketDataRepository marketData;
	private final HoldingCalculator holdings;

	public GetPortfolioUseCaseImpl(UserRepository users, OrderRepository orders, InstrumentRepository instruments,
								   MarketDataRepository marketData, HoldingCalculator holdings) {
		this.users = users;
		this.orders = orders;
		this.instruments = instruments;
		this.marketData = marketData;
		this.holdings = holdings;
	}

	@Override
	public Portfolio get(Long userId) {
		User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
		List<Order> history = orders.findByUserId(userId);
		List<Long> instrumentIds = history.stream()
				.map(Order::instrumentId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		Map<Long, Instrument> byId = instruments.findByIds(instrumentIds).stream()
				.collect(Collectors.toMap(Instrument::id, Function.identity()));
		Holdings calculated = holdings.calculate(history, byId, marketData.findLatestByInstrumentIds(instrumentIds));
		return new Portfolio(user.id(), user.accountNumber(), calculated.totalValue(), calculated.availableCash(),
				calculated.positions());
	}
}
