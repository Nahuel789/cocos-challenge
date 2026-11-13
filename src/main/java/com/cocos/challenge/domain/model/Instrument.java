package com.cocos.challenge.domain.model;

public record Instrument(Long id, String ticker, String name, InstrumentType type, Long currencyId) {
}
