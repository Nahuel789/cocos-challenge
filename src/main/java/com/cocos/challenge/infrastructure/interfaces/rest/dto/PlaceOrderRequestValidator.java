package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Decimals;
import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlaceOrderRequestValidator implements ConstraintValidator<ValidPlaceOrder, PlaceOrderRequest> {

	@Override
	public boolean isValid(PlaceOrderRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		boolean valid = true;
		if (request.side() != null && request.side() != OrderSide.BUY && request.side() != OrderSide.SELL) {
			valid = violation(context, "side", "side must be BUY or SELL");
		}
		boolean hasQuantity = request.quantity() != null;
		boolean hasAmount = request.amount() != null;
		if (hasQuantity == hasAmount) {
			valid = violation(context, "quantity", "Provide quantity or amount, only one of them") && valid;
		}
		if (hasQuantity && !Decimals.isWhole(request.quantity())) {
			valid = violation(context, "quantity", "quantity must be a whole number of shares") && valid;
		}
		if (request.type() == OrderType.LIMIT && (request.price() == null || request.price().signum() <= 0)) {
			valid = violation(context, "price", "LIMIT orders require price") && valid;
		}
		return valid;
	}

	private static boolean violation(ConstraintValidatorContext context, String field, String message) {
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(field)
				.addConstraintViolation();
		return false;
	}
}
