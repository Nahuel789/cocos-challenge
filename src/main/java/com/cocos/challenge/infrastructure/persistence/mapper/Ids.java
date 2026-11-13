package com.cocos.challenge.infrastructure.persistence.mapper;

import java.util.Collection;
import java.util.List;

public final class Ids {

	private Ids() {
	}

	public static Integer db(Long id) {
		return id == null ? null : Math.toIntExact(id);
	}

	public static Long domain(Integer id) {
		return id == null ? null : id.longValue();
	}

	public static List<Integer> db(Collection<Long> ids) {
		return ids.stream().map(Ids::db).toList();
	}
}
