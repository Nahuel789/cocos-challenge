package com.cocos.challenge.infrastructure.persistence.adapter;

import com.cocos.challenge.domain.model.Instrument;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;
import com.cocos.challenge.domain.repositories.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.mapper.Ids;
import com.cocos.challenge.infrastructure.persistence.mapper.Pages;
import com.cocos.challenge.infrastructure.persistence.mapper.PersistenceMapper;
import com.cocos.challenge.infrastructure.persistence.repository.SpringInstrumentRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class InstrumentRepositoryAdapter implements InstrumentRepository {

	private final SpringInstrumentRepository instruments;

	public InstrumentRepositoryAdapter(SpringInstrumentRepository instruments) {
		this.instruments = instruments;
	}

	@Override
	public Optional<Instrument> findById(Long id) {
		return instruments.findById(Ids.db(id)).map(PersistenceMapper::toDomain);
	}

	@Override
	public List<Instrument> findByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return instruments.findAllById(Ids.db(ids)).stream().map(PersistenceMapper::toDomain).toList();
	}

	@Override
	public Page<Instrument> search(String ticker, String name, PageQuery pageQuery) {
		return Pages.domain(instruments.search(ticker, name, Pages.db(pageQuery)), PersistenceMapper::toDomain);
	}
}
