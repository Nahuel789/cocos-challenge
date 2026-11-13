package com.cocos.challenge.domain.repositories;

import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

	Order save(Order order);

	Optional<Order> findById(Long id);

	List<Order> findByUserId(Long userId);

	Page<Order> findByUserId(Long userId, PageQuery pageQuery);
}
