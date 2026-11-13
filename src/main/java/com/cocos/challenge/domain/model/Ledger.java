package com.cocos.challenge.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Ledger {

	private BigDecimal settled = BigDecimal.ZERO;
	private BigDecimal reservedCash = BigDecimal.ZERO;
	private final Map<Long, Lot> lots = new HashMap<>();

	public void record(Order order) {
		if (order.isCashMovement()) {
			applyCash(order);
			return;
		}
		switch (order.status()) {
			case FILLED -> fill(order);
			case NEW -> reserve(order);
			case REJECTED, CANCELLED -> {
			}
		}
	}

	public BigDecimal settled() {
		return settled;
	}

	public BigDecimal availableCash() {
		return settled.subtract(reservedCash);
	}

	public Map<Long, Lot> lots() {
		return Collections.unmodifiableMap(lots);
	}

	private void applyCash(Order order) {
		if (order.status() != OrderStatus.FILLED) {
			return;
		}
		BigDecimal size = order.sizeOrZero();
		settled = order.side() == OrderSide.CASH_IN ? settled.add(size) : settled.subtract(size);
	}

	private void fill(Order order) {
		Lot lot = lot(order.instrumentId());
		if (order.side() == OrderSide.BUY) {
			settled = settled.subtract(order.notional());
			lot.buy(order.sizeOrZero(), order.priceOrZero());
			return;
		}
		if (order.side() == OrderSide.SELL) {
			lot.sell(order.sizeOrZero());
			settled = settled.add(order.notional());
		}
	}

	private void reserve(Order order) {
		if (order.side() == OrderSide.BUY) {
			lot(order.instrumentId());
			reservedCash = reservedCash.add(order.notional());
			return;
		}
		if (order.side() == OrderSide.SELL) {
			lot(order.instrumentId()).reserve(order.sizeOrZero());
		}
	}

	private Lot lot(Long instrumentId) {
		return lots.computeIfAbsent(instrumentId, ignored -> new Lot());
	}
}
