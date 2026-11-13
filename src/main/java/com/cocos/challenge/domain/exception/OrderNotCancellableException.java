package com.cocos.challenge.domain.exception;

public final class OrderNotCancellableException extends DomainException {

	public OrderNotCancellableException(Long orderId) {
		super("CONFLICT", "Only orders in NEW status can be cancelled. Order " + orderId);
	}
}
