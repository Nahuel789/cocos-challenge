package com.cocos.challenge.application.service;

import com.cocos.challenge.domain.command.PlaceOrderCommand;
import com.cocos.challenge.domain.exception.InvalidOrderRequestException;
import com.cocos.challenge.domain.model.Decimals;
import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderType;

public final class PlaceOrderValidator {

	public void validate(PlaceOrderCommand command) {
		if (command.userId() == null || command.instrumentId() == null || command.side() == null || command.type() == null) {
			throw new InvalidOrderRequestException("Required order fields are missing");
		}
		if (command.side() != OrderSide.BUY && command.side() != OrderSide.SELL) {
			throw new InvalidOrderRequestException("side must be BUY or SELL");
		}
		boolean hasQuantity = command.quantity() != null;
		boolean hasAmount = command.amount() != null;
		if (hasQuantity == hasAmount) {
			throw new InvalidOrderRequestException("Provide quantity or amount, only one of them");
		}
		if (hasQuantity && (command.quantity().signum() <= 0 || !Decimals.isWhole(command.quantity()))) {
			throw new InvalidOrderRequestException("quantity must be a whole number of shares");
		}
		if (hasAmount && command.amount().signum() <= 0) {
			throw new InvalidOrderRequestException("amount must be greater than zero");
		}
		if (command.type() == OrderType.LIMIT && (command.price() == null || command.price().signum() <= 0)) {
			throw new InvalidOrderRequestException("LIMIT orders require price");
		}
	}
}
