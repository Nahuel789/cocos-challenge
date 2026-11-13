package com.cocos.challenge.infrastructure.interfaces.rest;

import com.cocos.challenge.domain.usecases.GetPortfolioUseCase;
import com.cocos.challenge.infrastructure.interfaces.rest.apidocs.GetPortfolioOperation;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.PortfolioResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class PortfolioController {

	private final GetPortfolioUseCase portfolios;

	public PortfolioController(GetPortfolioUseCase portfolios) {
		this.portfolios = portfolios;
	}

	@GetMapping("/api/users/{userId}/portfolio")
	@GetPortfolioOperation
	public ResponseEntity<PortfolioResponse> get(@PathVariable Long userId) {
		try {
			return ResponseEntity.ok(PortfolioResponse.from(portfolios.get(userId)));
		} catch (RuntimeException ex) {
			log.error("GET /api/users/{}/portfolio - failed: {} - {}",
					userId, ex.getClass().getSimpleName(), ex.getMessage());
			throw ex;
		}
	}
}
