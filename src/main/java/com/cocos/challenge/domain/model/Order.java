package com.cocos.challenge.domain.model;

import com.cocos.challenge.domain.exception.OrderNotCancellableException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;

public record Order(
		Long id,
		Long instrumentId,
		Long currencyId,
		Long userId,
		OrderSide side,
		BigDecimal size,
		BigDecimal price,
		OrderType type,
		OrderStatus status,
		Instant datetime
) {

	public static final Comparator<Order> CHRONOLOGICAL = Comparator
			.comparing(Order::datetime, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(order -> order.id() == null ? Long.MAX_VALUE : order.id());

	public static Order filled(Long userId, Long instrumentId, Long currencyId, OrderSide side, OrderType type,
			BigDecimal size, BigDecimal price, Instant datetime) {
		return new Order(null, instrumentId, currencyId, userId, side, size, price, type, OrderStatus.FILLED, datetime);
	}

	public static Order pending(Long userId, Long instrumentId, Long currencyId, OrderSide side, BigDecimal size,
			BigDecimal price, Instant datetime) {
		return new Order(null, instrumentId, currencyId, userId, side, size, price, OrderType.LIMIT, OrderStatus.NEW,
				datetime);
	}

	public static Order rejected(Long userId, Long instrumentId, Long currencyId, OrderSide side, OrderType type,
			BigDecimal size, BigDecimal price, Instant datetime) {
		return new Order(null, instrumentId, currencyId, userId, side, size, price, type, OrderStatus.REJECTED, datetime);
	}

	public Order cancel() {
		if (status != OrderStatus.NEW) {
			throw new OrderNotCancellableException(id);
		}
		return new Order(id, instrumentId, currencyId, userId, side, size, price, type, OrderStatus.CANCELLED, datetime);
	}

	public BigDecimal sizeOrZero() {
		return size == null ? BigDecimal.ZERO : size;
	}

	public BigDecimal priceOrZero() {
		return price == null ? BigDecimal.ZERO : price;
	}

	public BigDecimal notional() {
		return sizeOrZero().multiply(priceOrZero());
	}

	public boolean isCashMovement() {
		return side == OrderSide.CASH_IN || side == OrderSide.CASH_OUT;
	}
}
