package com.cocos.challenge.application.service;

import com.cocos.challenge.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HoldingCalculatorTest {

	private final HoldingCalculator calculator = new HoldingCalculator();

	private static final long ARS = 1L;

	private final Instrument ggal = new Instrument(2L, "GGAL", "Grupo Financiero Galicia", InstrumentType.ACCIONES, ARS);
	private final Instrument ypfd = new Instrument(3L, "YPFD", "YPF", InstrumentType.ACCIONES, ARS);

	@Test
	void seedHoldingsMatchTheDemoAccount() {
		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "1500000", "2026-11-07T12:00:00Z"),
						filledBuy(2L, ggal.id(), "10", "8700", "2026-11-08T15:30:00Z")),
				Map.of(ggal.id(), ggal),
				Map.of(ggal.id(), new Quote(ggal.id(), decimal("8700"), decimal("8500"))));

		assertThat(holdings.settledCash()).isEqualByComparingTo("1413000.00");
		assertThat(holdings.availableCash()).isEqualByComparingTo("1413000.00");
		assertThat(holdings.totalValue()).isEqualByComparingTo("1500000.00");
		assertThat(holdings.positions()).hasSize(1);
		Position position = holdings.positions().getFirst();
		assertThat(position.ticker()).isEqualTo("GGAL");
		assertThat(position.quantity()).isEqualByComparingTo("10");
		assertThat(position.marketValue()).isEqualByComparingTo("87000.00");
		assertThat(position.averagePrice()).isEqualByComparingTo("8700");
		assertThat(position.totalReturnPercent()).isEqualByComparingTo("0");
		assertThat(position.dailyReturnPercent()).isEqualByComparingTo("2.3529");
	}

	@Test
	void weightedAverageSurvivesAPartialSaleAndAnotherInstrumentStaysUntouched() {
		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "10000", "2026-11-01T12:00:00Z"),
						filledBuy(2L, ggal.id(), "10", "100", "2026-11-02T12:00:00Z"),
						filledBuy(3L, ggal.id(), "10", "200", "2026-11-03T12:00:00Z"),
						filledSell(4L, ggal.id(), "4", "150", "2026-11-04T12:00:00Z"),
						filledBuy(5L, ypfd.id(), "2", "50", "2026-11-05T12:00:00Z")),
				Map.of(ggal.id(), ggal, ypfd.id(), ypfd),
				Map.of(
						ggal.id(), new Quote(ggal.id(), decimal("180"), decimal("150")),
						ypfd.id(), new Quote(ypfd.id(), decimal("60"), decimal("50"))));

		Position ggalPosition = position(holdings, "GGAL");
		assertThat(ggalPosition.quantity()).isEqualByComparingTo("16");
		assertThat(ggalPosition.averagePrice()).isEqualByComparingTo("150");
		assertThat(ggalPosition.marketValue()).isEqualByComparingTo("2880.00");
		assertThat(ggalPosition.totalReturnPercent()).isEqualByComparingTo("20");
		assertThat(position(holdings, "YPFD").quantity()).isEqualByComparingTo("2");
		assertThat(holdings.settledCash()).isEqualByComparingTo("7500.00");
		assertThat(holdings.totalValue()).isEqualByComparingTo("10500.00");
	}

	@Test
	void openLimitReservesCashWithoutReducingTotalValue() {
		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "1000", "2026-11-01T12:00:00Z"),
						pendingBuy(2L, ggal.id(), "2", "100", "2026-11-02T12:00:00Z")),
				Map.of(ggal.id(), ggal),
				Map.of(ggal.id(), new Quote(ggal.id(), decimal("100"), decimal("90"))));

		assertThat(holdings.settledCash()).isEqualByComparingTo("1000.00");
		assertThat(holdings.availableCash()).isEqualByComparingTo("800.00");
		assertThat(holdings.totalValue()).isEqualByComparingTo("1000.00");
		assertThat(holdings.positions()).isEmpty();
	}

	@Test
	void openSellReservesSharesButKeepsThePosition() {
		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "5000", "2026-11-01T12:00:00Z"),
						filledBuy(2L, ggal.id(), "10", "100", "2026-11-02T12:00:00Z"),
						pendingSell(3L, ggal.id(), "4", "120", "2026-11-03T12:00:00Z")),
				Map.of(ggal.id(), ggal),
				Map.of(ggal.id(), new Quote(ggal.id(), decimal("110"), decimal("100"))));

		assertThat(holdings.availableShares().get(ggal.id())).isEqualByComparingTo("6");
		assertThat(position(holdings, "GGAL").quantity()).isEqualByComparingTo("10");
	}

	@Test
	void rejectedCancelledAndCashOutDoNotInventHoldings() {
		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "1000", "2026-11-01T12:00:00Z"),
						cashOut(2L, "200", "2026-11-02T12:00:00Z"),
						rejectedBuy(3L, ggal.id(), "100", "100", "2026-11-03T12:00:00Z"),
						cancelledBuy(4L, ggal.id(), "5", "100", "2026-11-04T12:00:00Z")),
				Map.of(ggal.id(), ggal),
				Map.of());

		assertThat(holdings.availableCash()).isEqualByComparingTo("800.00");
		assertThat(holdings.positions()).isEmpty();
		assertThat(holdings.availableShares()).isEmpty();
	}

	@Test
	void missingOrZeroPreviousCloseHasNoDailyReturn() {
		assertThat(new Quote(1L, decimal("10"), null).dailyReturnPercent()).isNull();
		assertThat(new Quote(1L, decimal("10"), BigDecimal.ZERO).dailyReturnPercent()).isNull();

		Holdings holdings = calculator.calculate(
				List.of(
						cashIn(1L, "1000", "2026-11-01T12:00:00Z"),
						filledBuy(2L, ggal.id(), "1", "10", "2026-11-02T12:00:00Z")),
				Map.of(ggal.id(), ggal),
				Map.of(ggal.id(), new Quote(ggal.id(), decimal("12"), null)));

		assertThat(position(holdings, "GGAL").dailyReturnPercent()).isNull();
		assertThat(position(holdings, "GGAL").totalReturnPercent()).isEqualByComparingTo("20");
	}

	private static Position position(Holdings holdings, String ticker) {
		return holdings.positions().stream()
				.filter(position -> position.ticker().equals(ticker))
				.findFirst()
				.orElseThrow();
	}

	private Order cashIn(Long id, String size, String datetime) {
		return order(id, null, OrderSide.CASH_IN, null, size, "1", OrderStatus.FILLED, datetime);
	}

	private Order cashOut(Long id, String size, String datetime) {
		return order(id, null, OrderSide.CASH_OUT, null, size, "1", OrderStatus.FILLED, datetime);
	}

	private Order filledBuy(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.BUY, OrderType.MARKET, size, price, OrderStatus.FILLED, datetime);
	}

	private Order filledSell(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.SELL, OrderType.MARKET, size, price, OrderStatus.FILLED, datetime);
	}

	private Order pendingBuy(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.BUY, OrderType.LIMIT, size, price, OrderStatus.NEW, datetime);
	}

	private Order pendingSell(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.SELL, OrderType.LIMIT, size, price, OrderStatus.NEW, datetime);
	}

	private Order rejectedBuy(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.BUY, OrderType.MARKET, size, price, OrderStatus.REJECTED, datetime);
	}

	private Order cancelledBuy(Long id, Long instrumentId, String size, String price, String datetime) {
		return order(id, instrumentId, OrderSide.BUY, OrderType.LIMIT, size, price, OrderStatus.CANCELLED, datetime);
	}

	private static Order order(Long id, Long instrumentId, OrderSide side, OrderType type, String size, String price,
			OrderStatus status, String datetime) {
		return new Order(id, instrumentId, ARS, 1L, side, decimal(size), decimal(price), type, status,
				Instant.parse(datetime));
	}

	private static BigDecimal decimal(String value) {
		return new BigDecimal(value);
	}
}
