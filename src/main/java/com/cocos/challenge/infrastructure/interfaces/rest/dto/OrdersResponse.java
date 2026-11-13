package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.Page;

import java.util.List;

public record OrdersResponse(List<OrderResponse> orders, PageResponse page) {

	public static OrdersResponse from(Page<Order> page) {
		return new OrdersResponse(page.content().stream().map(OrderResponse::from).toList(), PageResponse.from(page));
	}
}
