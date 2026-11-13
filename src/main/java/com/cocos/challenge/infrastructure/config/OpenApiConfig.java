package com.cocos.challenge.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI cocosOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Cocos API")
				.version("v1")
				.description("""
						Portfolio, asset search, and order submission.
						A MARKET order executes at the close and is stored as FILLED.
						A LIMIT order stays NEW until the user cancels it.
						If cash or holdings are insufficient, the order is stored as REJECTED.
						"""));
	}
}
