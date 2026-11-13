package com.cocos.challenge.infrastructure.persistence.mapper;

import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;
import org.springframework.data.domain.PageRequest;

import java.util.function.Function;

public final class Pages {

	private Pages() {
	}

	public static PageRequest db(PageQuery query) {
		return PageRequest.of(query.page(), query.size());
	}

	public static <E, T> Page<T> domain(org.springframework.data.domain.Page<E> page, Function<? super E, ? extends T> mapper) {
		return new Page<>(
				page.getContent().stream().<T>map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements());
	}
}
