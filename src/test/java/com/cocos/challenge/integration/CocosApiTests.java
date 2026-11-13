package com.cocos.challenge.integration;

import com.cocos.challenge.config.PostgresTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "cocos.tests.keep-seed=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestcontainersConfig.class)
class CocosApiTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Autowired
	private DataSource dataSource;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayAppliesSchemaAndSeed() throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			List<String> versions = new ArrayList<>();
			try (ResultSet rows = statement.executeQuery(
					"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")) {
				while (rows.next()) {
					versions.add(rows.getString("version"));
				}
			}
			assertEquals(List.of("1", "2"), versions);

			try (ResultSet rows = statement.executeQuery("""
					SELECT
						(SELECT COUNT(*) FROM currencies WHERE code = 'ARS') AS currencies,
						(SELECT COUNT(*) FROM instruments WHERE ticker = 'ARS') AS ars_instruments,
						(SELECT COUNT(*) FROM instruments i JOIN currencies c ON c.id = i.currency_id WHERE c.code = 'ARS') AS priced_in_ars,
						(SELECT COUNT(*) FROM orders WHERE side IN ('CASH_IN', 'CASH_OUT') AND instrumentid IS NULL) AS cash_moves,
						(SELECT COUNT(*) FROM marketdata) AS prices,
						(SELECT COUNT(*) FROM orders WHERE side = 'CASH_IN' AND status = 'FILLED') AS cash_ins,
						(SELECT COUNT(*) FROM orders WHERE side = 'BUY' AND status = 'FILLED') AS buys
					""")) {
				assertTrue(rows.next());
				assertEquals(1, rows.getInt("currencies"));
				assertEquals(0, rows.getInt("ars_instruments"));
				assertEquals(65, rows.getInt("priced_in_ars"));
				assertEquals(2, rows.getInt("cash_moves"));
				assertEquals(126, rows.getInt("prices"));
				assertEquals(1, rows.getInt("cash_ins"));
				assertEquals(3, rows.getInt("buys"));
				assertFalse(rows.next());
			}
		}
	}

	@Test
	void unfilteredSearchReturnsTheFirstPageOfTheSeed() throws Exception {
		mockMvc.perform(get("/api/instruments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instruments", hasSize(20)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("AGRO"))
				.andExpect(jsonPath("$.instruments[19].ticker").value("CVH"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(65))
				.andExpect(jsonPath("$.page.totalPages").value(4));

		mockMvc.perform(get("/api/instruments").param("page", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instruments", hasSize(5)))
				.andExpect(jsonPath("$.instruments[0].ticker").value("TGSU2"))
				.andExpect(jsonPath("$.instruments[4].ticker").value("YPFD"))
				.andExpect(jsonPath("$.page.number").value(3))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(65));
	}

	@Test
	void ordersWithoutPaginationReturnTheFirstPage() throws Exception {
		mockMvc.perform(get("/api/users/1/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orders", hasSize(11)))
				.andExpect(jsonPath("$.orders[0].side").value("SELL"))
				.andExpect(jsonPath("$.orders[0].instrumentId").value(31))
				.andExpect(jsonPath("$.orders[0].size").value(30))
				.andExpect(jsonPath("$.orders[10].side").value("CASH_IN"))
				.andExpect(jsonPath("$.orders[10].size").value(1000000))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(11))
				.andExpect(jsonPath("$.page.totalPages").value(1));
	}
}
