package com.cocos.challenge.domain.exception;

public final class UserNotFoundException extends DomainException {

	public UserNotFoundException(Long userId) {
		super("NOT_FOUND", "User " + userId + " does not exist");
	}
}
