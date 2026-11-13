package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import com.cocos.challenge.domain.command.PlaceOrderCommand;
import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@ValidPlaceOrder
@Schema(description = "Creates an order. Provide quantity or amount, never both. MARKET ignores price.")
public record PlaceOrderRequest(
		@NotNull @Schema(example = "1") Long userId,
		@NotNull @Schema(example = "2") Long instrumentId,
		@NotNull @Schema(example = "BUY") OrderSide side,
		@NotNull @Schema(example = "MARKET") OrderType type,
		@Positive @Schema(example = "10", description = "Whole number of shares") BigDecimal quantity,
		@Positive @Schema(example = "15000.00", description = "Amount in pesos. Converted to whole shares using floor.") BigDecimal amount,
		@Schema(example = "8700.00", description = "Required for LIMIT. Ignored for MARKET.") BigDecimal price
) {

	public PlaceOrderCommand toCommand() {
		return new PlaceOrderCommand(userId, instrumentId, side, type, quantity, amount, price);
	}
}
