package com.cocos.challenge.application.usecases;

import com.cocos.challenge.domain.exception.UserNotFoundException;
import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;
import com.cocos.challenge.domain.repositories.OrderRepository;
import com.cocos.challenge.domain.repositories.UserRepository;
import com.cocos.challenge.domain.usecases.ListOrdersUseCase;

public final class ListOrdersUseCaseImpl implements ListOrdersUseCase {

	private final UserRepository users;
	private final OrderRepository orders;

	public ListOrdersUseCaseImpl(UserRepository users, OrderRepository orders) {
		this.users = users;
		this.orders = orders;
	}

	@Override
	public Page<Order> list(Long userId, PageQuery pageQuery) {
		if (users.findById(userId).isEmpty()) {
			throw new UserNotFoundException(userId);
		}
		return orders.findByUserId(userId, pageQuery);
	}
}
