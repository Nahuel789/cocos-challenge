package com.cocos.challenge.domain.model;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

	public Page {
		content = List.copyOf(content);
	}

	public int totalPages() {
		return (int) ((totalElements + size - 1) / size);
	}

	public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
		return new Page<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
	}
}
