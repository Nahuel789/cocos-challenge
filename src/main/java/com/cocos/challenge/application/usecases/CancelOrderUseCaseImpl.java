package com.cocos.challenge.application.usecases;

import com.cocos.challenge.domain.exception.OrderNotFoundException;
import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.repositories.OrderRepository;
import com.cocos.challenge.domain.repositories.TransactionRunner;
import com.cocos.challenge.domain.repositories.UserRepository;
import com.cocos.challenge.domain.usecases.CancelOrderUseCase;

public final class CancelOrderUseCaseImpl implements CancelOrderUseCase {

	private final OrderRepository orders;
	private final UserRepository users;
	private final TransactionRunner transactions;

	public CancelOrderUseCaseImpl(OrderRepository orders, UserRepository users, TransactionRunner transactions) {
		this.orders = orders;
		this.users = users;
		this.transactions = transactions;
	}

	@Override
	public Order cancel(Long orderId) {
		return transactions.run(() -> {
			Order current = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
			users.lockById(current.userId());
			Order locked = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
			return orders.save(locked.cancel());
		});
	}
}
