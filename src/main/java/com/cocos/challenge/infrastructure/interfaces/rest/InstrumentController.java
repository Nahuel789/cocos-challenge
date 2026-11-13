package com.cocos.challenge.infrastructure.interfaces.rest;

import com.cocos.challenge.domain.model.PageQuery;
import com.cocos.challenge.domain.usecases.SearchInstrumentsUseCase;
import com.cocos.challenge.infrastructure.interfaces.rest.apidocs.SearchInstrumentsOperation;
import com.cocos.challenge.infrastructure.interfaces.rest.dto.InstrumentsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class InstrumentController {

	private final SearchInstrumentsUseCase search;

	public InstrumentController(SearchInstrumentsUseCase search) {
		this.search = search;
	}

	@GetMapping("/api/instruments")
	@SearchInstrumentsOperation
	public ResponseEntity<InstrumentsResponse> search(
			@RequestParam(name = "ticker", required = false) String ticker,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
			@RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        try {
			InstrumentsResponse response = InstrumentsResponse.from(search.search(ticker, name, new PageQuery(page, size)));
			return ResponseEntity.ok(response);
		} catch (RuntimeException ex) {
			log.error("GET /api/instruments - failed for ticker={}, name={}, page={}, size={}: {} - {}",
					ticker, name, page, size, ex.getClass().getSimpleName(), ex.getMessage());
			throw ex;
		}
	}
}
