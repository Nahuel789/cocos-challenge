package com.cocos.challenge.infrastructure.persistence.entity;

import com.cocos.challenge.domain.model.OrderSide;
import com.cocos.challenge.domain.model.OrderStatus;
import com.cocos.challenge.domain.model.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "instrumentid")
	private Integer instrumentId;

	@Column(name = "currency_id", nullable = false)
	private Integer currencyId;

	@Column(name = "userid")
	private Integer userId;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private OrderSide side;

	private Integer size;

	@Column(precision = 10, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private OrderType type;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private OrderStatus status;

	private LocalDateTime datetime;
}
