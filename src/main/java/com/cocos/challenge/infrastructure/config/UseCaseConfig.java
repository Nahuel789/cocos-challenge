package com.cocos.challenge.infrastructure.config;

import com.cocos.challenge.application.service.HoldingCalculator;
import com.cocos.challenge.application.service.PlaceOrderValidator;
import com.cocos.challenge.application.usecases.*;
import com.cocos.challenge.domain.repositories.*;
import com.cocos.challenge.domain.usecases.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	HoldingCalculator holdingCalculator() {
		return new HoldingCalculator();
	}

	@Bean
	PlaceOrderValidator placeOrderValidator() {
		return new PlaceOrderValidator();
	}

	@Bean
	PlaceOrderUseCase placeOrderUseCase(UserRepository users, InstrumentRepository instruments, OrderRepository orders,
			MarketDataRepository marketData, TransactionRunner transactions, HoldingCalculator holdings,
			PlaceOrderValidator validator, Clock clock) {
		return new PlaceOrderUseCaseImpl(users, instruments, orders, marketData, transactions, holdings, validator, clock);
	}

	@Bean
	CancelOrderUseCase cancelOrderUseCase(OrderRepository orders, UserRepository users, TransactionRunner transactions) {
		return new CancelOrderUseCaseImpl(orders, users, transactions);
	}

	@Bean
	GetPortfolioUseCase getPortfolioUseCase(UserRepository users, OrderRepository orders, InstrumentRepository instruments,
			MarketDataRepository marketData, HoldingCalculator holdings) {
		return new GetPortfolioUseCaseImpl(users, orders, instruments, marketData, holdings);
	}

	@Bean
	SearchInstrumentsUseCase searchInstrumentsUseCase(InstrumentRepository instruments, MarketDataRepository marketData) {
		return new SearchInstrumentsUseCaseImpl(instruments, marketData);
	}

	@Bean
	ListOrdersUseCase listOrdersUseCase(UserRepository users, OrderRepository orders) {
		return new ListOrdersUseCaseImpl(users, orders);
	}
}
