package com.cocos.challenge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "marketdata")
public class MarketDataEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "instrumentid")
	private Integer instrumentId;

	@Column(precision = 10, scale = 2)
	private BigDecimal high;

	@Column(precision = 10, scale = 2)
	private BigDecimal low;

	@Column(precision = 10, scale = 2)
	private BigDecimal open;

	@Column(precision = 10, scale = 2)
	private BigDecimal close;

	@Column(name = "previousclose", precision = 10, scale = 2)
	private BigDecimal previousClose;

	@Column(name = "date")
	private LocalDate marketDate;
}
