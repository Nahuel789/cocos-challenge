package com.cocos.challenge.domain.usecases;

import com.cocos.challenge.domain.model.Order;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;

public interface ListOrdersUseCase {

	Page<Order> list(Long userId, PageQuery pageQuery);
}
