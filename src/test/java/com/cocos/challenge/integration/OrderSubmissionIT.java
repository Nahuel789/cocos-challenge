package com.cocos.challenge.integration;

import com.cocos.challenge.config.PostgresTestcontainersConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class OrderSubmissionIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@Test
	void marketBuyFillsAtCloseAndUpdatesPortfolio() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":2,"price":1}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.type").value("MARKET"))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.price").value(100.0));

		mockMvc.perform(get("/api/users/10/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCash").value(9300.0))
				.andExpect(jsonPath("$.totalValue").value(10000.0))
				.andExpect(jsonPath("$.positions[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.positions[0].quantity").value(7))
				.andExpect(jsonPath("$.positions[0].marketValue").value(700.0));
	}

	@Test
	void limitBuyStaysNewAndReservesCash() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"LIMIT","quantity":10,"price":100}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.price").value(100.0));

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":90}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void limitSellStaysNewAndReservesShares() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"SELL","type":"LIMIT","quantity":4,"price":120}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.type").value("LIMIT"))
				.andExpect(jsonPath("$.size").value(4));

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"SELL","type":"MARKET","quantity":2}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		mockMvc.perform(get("/api/users/10/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.positions[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$.positions[0].quantity").value(5));
	}

	@Test
	void orderPlacementWaitsWhileTheUserRowIsLocked() throws Exception {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT id FROM users WHERE id = 10 FOR UPDATE");
					ResultSet rows = statement.executeQuery()) {
				assertThat(rows.next()).isTrue();
			}

			Future<MvcResult> pending = pool.submit(() -> mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1}
									"""))
					.andReturn());

			assertThatThrownBy(() -> pending.get(2, TimeUnit.SECONDS))
					.isInstanceOf(TimeoutException.class);

			connection.commit();

			MvcResult result = pending.get(10, TimeUnit.SECONDS);
			assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
			assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.status"))
					.isEqualTo("FILLED");
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void marketAmountFloorsToWholeShares() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","amount":250}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.price").value(100.0));
	}

	@Test
	void buyAboveAvailableCashIsPersistedAsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1000}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(1000));

		mockMvc.perform(get("/api/users/10/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orders", hasSize(3)))
				.andExpect(jsonPath("$.orders[0].status").value("REJECTED"));
	}

	@Test
	void sellAboveHoldingsIsPersistedAsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"SELL","type":"MARKET","quantity":6}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void sellOfAnInstrumentNeverBoughtIsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":201,"side":"SELL","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void amountThatDoesNotCoverOneShareIsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","amount":50}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(0));
	}

	@Test
	void marketWithoutAPriceIsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":202,"side":"BUY","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void limitWithoutPriceDoesNotInsert() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"LIMIT","quantity":1}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/users/10/orders"))
				.andExpect(jsonPath("$.orders", hasSize(2)));
	}

	@Test
	void quantityAndAmountTogetherAreRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1,"amount":100}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/users/10/orders"))
				.andExpect(jsonPath("$.orders", hasSize(2)));
	}

	@Test
	void fractionalQuantityIsRejected() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1.5}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingUserOrInstrumentReturnsNotFound() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":999,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":999,"side":"BUY","type":"MARKET","quantity":1}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void cancellingANewOrderReleasesCashAndAFilledOrderCannotBeCancelled() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"LIMIT","quantity":10,"price":100}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andReturn();
		Number limitId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":90}
								"""))
				.andExpect(jsonPath("$.status").value("REJECTED"));

		mockMvc.perform(post("/api/orders/{id}/cancel", limitId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		MvcResult filled = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"instrumentId":200,"side":"BUY","type":"MARKET","quantity":90}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andReturn();
		Number filledId = JsonPath.read(filled.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/orders/{id}/cancel", filledId))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/orders/{id}/cancel", filledId))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/orders/{id}/cancel", 99999))
				.andExpect(status().isNotFound());
	}

	@Test
	void ordersArePaginatedNewestFirst() throws Exception {
		mockMvc.perform(get("/api/users/10/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orders", hasSize(2)))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.page.totalPages").value(1));

		mockMvc.perform(get("/api/users/10/orders").param("page", "0").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orders", hasSize(1)))
				.andExpect(jsonPath("$.orders[0].side").value("BUY"))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.page.totalPages").value(2));

		mockMvc.perform(get("/api/users/10/orders").param("page", "1").param("size", "1"))
				.andExpect(jsonPath("$.orders", hasSize(1)))
				.andExpect(jsonPath("$.orders[0].side").value("CASH_IN"))
				.andExpect(jsonPath("$.page.number").value(1));

		mockMvc.perform(get("/api/users/10/orders").param("page", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orders", hasSize(0)))
				.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void invalidPaginationIsRejected() throws Exception {
		mockMvc.perform(get("/api/users/10/orders").param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION"))
				.andExpect(jsonPath("$.violations[0].field").value("size"));

		mockMvc.perform(get("/api/users/10/orders").param("size", "101"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/users/10/orders").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.violations[0].field").value("page"));

		mockMvc.perform(get("/api/instruments").param("size", "101"))
				.andExpect(status().isBadRequest());
	}
}
