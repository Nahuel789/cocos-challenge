package com.cocos.challenge.infrastructure.interfaces.rest.apidocs;

import com.cocos.challenge.infrastructure.interfaces.rest.dto.ApiError;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
		summary = "Submit a buy or sell order",
		description = "After the order is persisted, publishes ORDER_PERSISTED so the consuming client queries the portfolio again and recalculates its assets.",
		tags = "Orders")
@ApiResponses({
		@ApiResponse(
				responseCode = "201",
				description = "Order stored. Status may be FILLED, NEW, or REJECTED.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
		@ApiResponse(
				responseCode = "400",
				description = "Invalid request.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(
				responseCode = "404",
				description = "User or instrument not found.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
})
public @interface PlaceOrderOperation {
}
