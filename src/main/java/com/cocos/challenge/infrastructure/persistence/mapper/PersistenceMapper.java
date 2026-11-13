package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.*;
import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.entity.OrderEntity;
import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class PersistenceMapper {

	private PersistenceMapper() {
	}

	public static User toDomain(UserEntity entity) {
		return new User(Ids.domain(entity.getId()), entity.getEmail(), entity.getAccountNumber());
	}

	public static Instrument toDomain(InstrumentEntity entity) {
		return new Instrument(Ids.domain(entity.getId()), entity.getTicker(), entity.getName(), entity.getType(),
				Ids.domain(entity.getCurrencyId()));
	}

	public static Quote toDomain(MarketDataEntity entity) {
		return new Quote(Ids.domain(entity.getInstrumentId()), entity.getClose(), entity.getPreviousClose());
	}

	public static Order toDomain(OrderEntity entity) {
		return new Order(
				Ids.domain(entity.getId()),
				Ids.domain(entity.getInstrumentId()),
				Ids.domain(entity.getCurrencyId()),
				Ids.domain(entity.getUserId()),
				entity.getSide(),
				entity.getSize() == null ? null : Decimals.quantity(BigDecimal.valueOf(entity.getSize())),
				entity.getPrice() == null ? null : Decimals.price(entity.getPrice()),
				entity.getType(),
				entity.getStatus(),
				entity.getDatetime() == null ? null : entity.getDatetime().toInstant(ZoneOffset.UTC));
	}

	public static OrderEntity toNewEntity(Order order) {
		OrderEntity entity = new OrderEntity();
		copy(order, entity);
		return entity;
	}

	public static void copy(Order order, OrderEntity entity) {
		entity.setInstrumentId(Ids.db(order.instrumentId()));
		entity.setCurrencyId(Ids.db(order.currencyId()));
		entity.setUserId(Ids.db(order.userId()));
		entity.setSide(order.side());
		entity.setSize(order.size() == null ? null : order.size().intValueExact());
		entity.setPrice(order.price());
		entity.setType(order.type());
		entity.setStatus(order.status());
		entity.setDatetime(order.datetime() == null ? null : LocalDateTime.ofInstant(order.datetime(), ZoneOffset.UTC));
	}
}
