package com.cocos.challenge.domain.usecases;

import com.cocos.challenge.domain.model.Order;

public interface CancelOrderUseCase {

	Order cancel(Long orderId);
}
