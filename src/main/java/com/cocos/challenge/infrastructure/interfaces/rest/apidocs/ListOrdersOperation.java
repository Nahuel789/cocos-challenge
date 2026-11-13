package com.cocos.challenge.infrastructure.interfaces.rest.apidocs;

import com.cocos.challenge.infrastructure.interfaces.rest.dto.ApiError;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.OrdersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
		summary = "List a user's orders",
		description = "With no page or size, returns the first page: page 0 and size 20, newest first. Size is at most 100.",
		tags = "Orders",
		parameters = {
				@Parameter(name = "page", description = "Zero-based page index. Defaults to 0."),
				@Parameter(name = "size", description = "Page size. Defaults to 20, maximum 100.")
		})
@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description = "One page of the user's orders, newest first. The default call returns the first 20.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrdersResponse.class))),
		@ApiResponse(
				responseCode = "400",
				description = "Invalid page or size.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(
				responseCode = "404",
				description = "User not found.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
})
public @interface ListOrdersOperation {
}
