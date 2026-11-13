package com.cocos.challenge.domain.exception;

public abstract class DomainException extends RuntimeException {

	private final String error;

	protected DomainException(String error, String message) {
		super(message);
		this.error = error;
	}

	public String error() {
		return error;
	}
}
