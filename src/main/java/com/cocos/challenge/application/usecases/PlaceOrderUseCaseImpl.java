package com.cocos.challenge.application.usecases;

import com.cocos.challenge.application.service.HoldingCalculator;
import com.cocos.challenge.application.service.PlaceOrderValidator;
import com.cocos.challenge.domain.command.PlaceOrderCommand;
import com.cocos.challenge.domain.exception.InstrumentNotFoundException;
import com.cocos.challenge.domain.model.*;
import com.cocos.challenge.domain.repositories.*;
import com.cocos.challenge.domain.usecases.PlaceOrderUseCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

	private final UserRepository users;
	private final InstrumentRepository instruments;
	private final OrderRepository orders;
	private final MarketDataRepository marketData;
	private final TransactionRunner transactions;
	private final HoldingCalculator holdings;
	private final PlaceOrderValidator validator;
	private final Clock clock;

	public PlaceOrderUseCaseImpl(UserRepository users, InstrumentRepository instruments, OrderRepository orders,
								 MarketDataRepository marketData, TransactionRunner transactions, HoldingCalculator holdings,
								 PlaceOrderValidator validator, Clock clock) {
		this.users = users;
		this.instruments = instruments;
		this.orders = orders;
		this.marketData = marketData;
		this.transactions = transactions;
		this.holdings = holdings;
		this.validator = validator;
		this.clock = clock;
	}

	@Override
	public Order place(PlaceOrderCommand command) {
		validator.validate(command);
		return transactions.run(() -> placeInTransaction(command));
	}

	private Order placeInTransaction(PlaceOrderCommand command) {
		users.lockById(command.userId());
		Instrument instrument = instruments.findById(command.instrumentId())
				.orElseThrow(() -> new InstrumentNotFoundException(command.instrumentId()));

		Optional<Quote> quote = marketData.findLatest(instrument.id());
		BigDecimal executionPrice = resolvePrice(command, quote);
		Instant now = Instant.now(clock);
		if (executionPrice == null) {
			return orders.save(Order.rejected(command.userId(), instrument.id(), instrument.currencyId(), command.side(),
					command.type(), requestedSize(command), null, now));
		}

		BigDecimal size;
		if (command.amount() != null) {
			size = command.amount().divide(executionPrice, 0, RoundingMode.DOWN);
			if (size.signum() <= 0) {
				return orders.save(Order.rejected(command.userId(), instrument.id(), instrument.currencyId(),
						command.side(), command.type(), BigDecimal.ZERO, executionPrice, now));
			}
		}
		else {
			size = command.quantity();
		}

		List<Order> history = orders.findByUserId(command.userId());
		List<Long> instrumentIds = instrumentIds(history);
		Holdings position = holdings.calculate(history, loadInstruments(instrumentIds),
				marketData.findLatestByInstrumentIds(instrumentIds));

		if (command.side() == OrderSide.BUY) {
			BigDecimal notional = size.multiply(executionPrice);
			if (notional.compareTo(position.availableCash()) > 0) {
				return orders.save(Order.rejected(command.userId(), instrument.id(), instrument.currencyId(),
						command.side(), command.type(), size, executionPrice, now));
			}
		}
		else {
			BigDecimal available = position.availableShares().getOrDefault(instrument.id(), BigDecimal.ZERO);
			if (size.compareTo(available) > 0) {
				return orders.save(Order.rejected(command.userId(), instrument.id(), instrument.currencyId(),
						command.side(), command.type(), size, executionPrice, now));
			}
		}

		Order accepted = command.type() == OrderType.MARKET
				? Order.filled(command.userId(), instrument.id(), instrument.currencyId(), command.side(),
						command.type(), size, executionPrice, now)
				: Order.pending(command.userId(), instrument.id(), instrument.currencyId(), command.side(), size,
						executionPrice, now);
		return orders.save(accepted);
	}

	private static BigDecimal resolvePrice(PlaceOrderCommand command, Optional<Quote> quote) {
		if (command.type() == OrderType.LIMIT) {
			return command.price();
		}
		if (quote.isEmpty() || quote.get().close() == null || quote.get().close().signum() <= 0) {
			return null;
		}
		return quote.get().close();
	}

	private static BigDecimal requestedSize(PlaceOrderCommand command) {
		return command.quantity() == null ? BigDecimal.ZERO : command.quantity();
	}

	private static List<Long> instrumentIds(List<Order> history) {
		return history.stream().map(Order::instrumentId).filter(Objects::nonNull).distinct().toList();
	}

	private Map<Long, Instrument> loadInstruments(List<Long> instrumentIds) {
		return instruments.findByIds(instrumentIds).stream()
				.collect(Collectors.toMap(Instrument::id, Function.identity()));
	}
}
