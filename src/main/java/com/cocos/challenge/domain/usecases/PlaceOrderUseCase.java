package com.cocos.challenge.domain.usecases;

import com.cocos.challenge.domain.command.PlaceOrderCommand;
import com.cocos.challenge.domain.model.Order;

public interface PlaceOrderUseCase {

	Order place(PlaceOrderCommand command);
}
