package com.cocos.challenge.infrastructure.persistence.entity;

import com.cocos.challenge.domain.model.InstrumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "instruments")
public class InstrumentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 10)
	private String ticker;

	@Column(length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private InstrumentType type;

	@Column(name = "currency_id", nullable = false)
	private Integer currencyId;
}
