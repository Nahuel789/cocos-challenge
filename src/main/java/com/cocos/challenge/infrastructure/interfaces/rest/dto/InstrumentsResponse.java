package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.InstrumentQuote;
import com.cocos.challenge.domain.model.Page;

import java.util.List;

public record InstrumentsResponse(List<InstrumentResponse> instruments, PageResponse page) {

	public static InstrumentsResponse from(Page<InstrumentQuote> page) {
		return new InstrumentsResponse(page.content().stream().map(InstrumentResponse::from).toList(), PageResponse.from(page));
	}
}
