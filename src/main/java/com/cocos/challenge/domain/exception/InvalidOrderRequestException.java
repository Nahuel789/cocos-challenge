package com.cocos.challenge.domain.exception;

public final class InvalidOrderRequestException extends DomainException {

	public InvalidOrderRequestException(String message) {
		super("VALIDATION", message);
	}
}
