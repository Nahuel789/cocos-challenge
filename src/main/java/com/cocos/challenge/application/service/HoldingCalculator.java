package com.cocos.challenge.application.service;

import com.cocos.challenge.domain.model.*;

import java.math.BigDecimal;
import java.util.*;

public final class HoldingCalculator {

	public Holdings calculate(List<Order> orders, Map<Long, Instrument> instruments, Map<Long, Quote> quotes) {
		Ledger ledger = buildLedger(orders, instruments);
		return buildHoldings(ledger, instruments, quotes);
	}

	private Ledger buildLedger(List<Order> orders, Map<Long, Instrument> instruments) {
		Ledger ledger = new Ledger();
		orders.stream()
				.sorted(Order.CHRONOLOGICAL)
				.forEach(order -> {
					if (!order.isCashMovement()) {
						requireInstrument(instruments, order.instrumentId());
					}
					ledger.record(order);
				});
		return ledger;
	}

	private Holdings buildHoldings(Ledger ledger, Map<Long, Instrument> instruments, Map<Long, Quote> quotes) {
		List<Position> positions = new ArrayList<>();
		Map<Long, BigDecimal> availableShares = new HashMap<>();
		BigDecimal marketSum = BigDecimal.ZERO;

		for (Map.Entry<Long, Lot> entry : ledger.lots().entrySet()) {
			Instrument instrument = instruments.get(entry.getKey());
			Lot lot = entry.getValue();
			availableShares.put(instrument.id(), lot.available());
			if (!lot.isOpen()) {
				continue;
			}
			Position position = Position.of(instrument, lot, quote(quotes, instrument.id()));
			marketSum = marketSum.add(position.marketValue());
			positions.add(position);
		}

		positions.sort(Comparator.comparing(Position::ticker));
		BigDecimal settled = Decimals.money(ledger.settled());
		return new Holdings(
				settled,
				Decimals.money(ledger.availableCash()),
				settled.add(marketSum),
				Map.copyOf(availableShares),
				List.copyOf(positions));
	}

	private static void requireInstrument(Map<Long, Instrument> instruments, Long instrumentId) {
		if (instruments.get(instrumentId) == null) {
			throw new IllegalArgumentException("Missing instrument " + instrumentId);
		}
	}

	private static Quote quote(Map<Long, Quote> quotes, Long instrumentId) {
		return quotes == null ? null : quotes.get(instrumentId);
	}
}
