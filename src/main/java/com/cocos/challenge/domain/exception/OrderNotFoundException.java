package com.cocos.challenge.domain.exception;

public final class OrderNotFoundException extends DomainException {

	public OrderNotFoundException(Long orderId) {
		super("NOT_FOUND", "Order " + orderId + " does not exist");
	}
}
