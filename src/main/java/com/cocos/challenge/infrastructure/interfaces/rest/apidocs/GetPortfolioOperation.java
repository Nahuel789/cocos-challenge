package com.cocos.challenge.infrastructure.interfaces.rest.apidocs;

import com.cocos.challenge.infrastructure.interfaces.rest.dto.ApiError;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.PortfolioResponse;
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
@Operation(summary = "Get a user's portfolio", tags = "Portfolio")
@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description = "Total value, available pesos, and positions.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortfolioResponse.class))),
		@ApiResponse(
				responseCode = "404",
				description = "User not found.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
})
public @interface GetPortfolioOperation {
}
