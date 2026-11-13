package com.cocos.challenge.infrastructure.interfaces.rest;

import com.cocos.challenge.domain.model.PageQuery;
import com.cocos.challenge.domain.usecases.CancelOrderUseCase;
import com.cocos.challenge.domain.usecases.ListOrdersUseCase;
import com.cocos.challenge.domain.usecases.PlaceOrderUseCase;
import com.cocos.challenge.infrastructure.interfaces.rest.apidocs.CancelOrderOperation;
import com.cocos.challenge.infrastructure.interfaces.rest.apidocs.ListOrdersOperation;
import com.cocos.challenge.infrastructure.interfaces.rest.apidocs.PlaceOrderOperation;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.OrderResponse;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.OrdersResponse;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
public class OrderController {

	private final PlaceOrderUseCase placeOrder;
	private final CancelOrderUseCase cancelOrder;
	private final ListOrdersUseCase listOrders;

	public OrderController(PlaceOrderUseCase placeOrder, CancelOrderUseCase cancelOrder, ListOrdersUseCase listOrders) {
		this.placeOrder = placeOrder;
		this.cancelOrder = cancelOrder;
		this.listOrders = listOrders;
	}

	@PostMapping("/api/orders")
	@PlaceOrderOperation
	public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(placeOrder.place(request.toCommand())));
		} catch (RuntimeException ex) {
			log.error("POST /api/orders - failed for userId={}, instrumentId={}: {} - {}",
					request.userId(), request.instrumentId(), ex.getClass().getSimpleName(), ex.getMessage());
			throw ex;
		}
	}

	@PostMapping("/api/orders/{orderId}/cancel")
	@CancelOrderOperation
	public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId) {
		try {
			return ResponseEntity.ok(OrderResponse.from(cancelOrder.cancel(orderId)));
		} catch (RuntimeException ex) {
			log.error("POST /api/orders/{}/cancel - failed: {} - {}",
					orderId, ex.getClass().getSimpleName(), ex.getMessage());
			throw ex;
		}
	}

	@GetMapping("/api/users/{userId}/orders")
	@ListOrdersOperation
	public ResponseEntity<OrdersResponse> list(
			@PathVariable Long userId,
			@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
			@RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
		try {
			return ResponseEntity.ok(OrdersResponse.from(listOrders.list(userId, new PageQuery(page, size))));
		} catch (RuntimeException ex) {
			log.error("GET /api/users/{}/orders - failed for page={}, size={}: {} - {}",
					userId, page, size, ex.getClass().getSimpleName(), ex.getMessage());
			throw ex;
		}
	}
}
