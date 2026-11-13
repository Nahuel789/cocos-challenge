package com.cocos.challenge.integration;

import com.cocos.challenge.config.PostgresTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfig.class)
@Sql(scripts = "/sql/orders-fixture.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PortfolioSearchIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void portfolioReportsCashMarketValueAndReturns() throws Exception {
		mockMvc.perform(get("/api/users/10/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(10))
				.andExpect(jsonPath("$.accountNumber").value("ACC-10"))
				.andExpect(jsonPath("$.availableCash").value(9500.0))
				.andExpect(jsonPath("$.totalValue").value(10000.0))
				.andExpect(jsonPath("$.positions", hasSize(1)))
				.andExpect(jsonPath("$.positions[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.positions[0].quantity").value(5))
				.andExpect(jsonPath("$.positions[0].marketPrice").value(100.0))
				.andExpect(jsonPath("$.positions[0].marketValue").value(500.0))
				.andExpect(jsonPath("$.positions[0].averagePrice").value(100.0))
				.andExpect(jsonPath("$.positions[0].totalReturnPercent").value(0.0))
				.andExpect(jsonPath("$.positions[0].dailyReturnPercent").value(11.1111));
	}

	@Test
	void buyingAnotherInstrumentDoesNotChangeTheFirstPosition() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":201,"side":"BUY","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.price").value(50.0));

		mockMvc.perform(get("/api/users/10/portfolio"))
				.andExpect(jsonPath("$.availableCash").value(9450.0))
				.andExpect(jsonPath("$.positions[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.positions[0].quantity").value(5))
				.andExpect(jsonPath("$.positions[1].ticker").value("YPFD"))
				.andExpect(jsonPath("$.positions[1].quantity").value(1));
	}

	@Test
	void unknownUserHasNoPortfolio() throws Exception {
		mockMvc.perform(get("/api/users/999/portfolio"))
				.andExpect(status().isNotFound());
	}

	@Test
	void searchMatchesTickerOrName() throws Exception {
		mockMvc.perform(get("/api/instruments").param("ticker", "ggal"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.instruments[0].lastPrice").value(100.0))
				.andExpect(jsonPath("$.instruments[0].dailyReturnPercent").value(11.1111));

		mockMvc.perform(get("/api/instruments").param("name", "YPF"))
				.andExpect(jsonPath("$.instruments[0].ticker").value("YPFD"));

		mockMvc.perform(get("/api/instruments").param("name", "galicia"))
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("GGAL"));

		mockMvc.perform(get("/api/instruments").param("ticker", "yp"))
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("YPFD"));

		mockMvc.perform(get("/api/instruments").param("name", "Grupo Financiero Galicia"))
				.andExpect(jsonPath("$.instruments[0].ticker").value("GGAL"));

		mockMvc.perform(get("/api/instruments").param("ticker", "GGAL").param("name", "Grupo Financiero Galicia"))
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("GGAL"));

		mockMvc.perform(get("/api/instruments").param("name", "Peso Argentino"))
				.andExpect(jsonPath("$.instruments", hasSize(0)));

		mockMvc.perform(get("/api/instruments"))
				.andExpect(jsonPath("$.instruments", hasSize(3)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("BMA"))
				.andExpect(jsonPath("$.instruments[1].ticker").value("GGAL"))
				.andExpect(jsonPath("$.instruments[2].ticker").value("YPFD"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@Test
	void searchIsPaginatedByTicker() throws Exception {
		mockMvc.perform(get("/api/instruments").param("page", "0").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instruments", hasSize(2)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("BMA"))
				.andExpect(jsonPath("$.instruments[1].ticker").value("GGAL"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(2));

		mockMvc.perform(get("/api/instruments").param("page", "1").param("size", "2"))
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("YPFD"))
				.andExpect(jsonPath("$.instruments[0].lastPrice").value(50.0));

		mockMvc.perform(get("/api/instruments").param("ticker", "g").param("size", "1"))
				.andExpect(jsonPath("$.instruments", hasSize(1)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.page.totalElements").value(1));
	}
}
