package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Page;

public record PageResponse(int number, int size, long totalElements, int totalPages) {

	public static PageResponse from(Page<?> page) {
		return new PageResponse(page.page(), page.size(), page.totalElements(), page.totalPages());
	}
}
