package com.cocos.challenge.domain.exception;

public final class InstrumentNotFoundException extends DomainException {

	public InstrumentNotFoundException(Long instrumentId) {
		super("NOT_FOUND", "Instrument " + instrumentId + " does not exist");
	}
}
