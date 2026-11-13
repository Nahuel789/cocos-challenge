package com.cocos.challenge.domain.repositories;

import com.cocos.challenge.domain.model.Instrument;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InstrumentRepository {

	Optional<Instrument> findById(Long id);

	List<Instrument> findByIds(Collection<Long> ids);

	Page<Instrument> search(String ticker, String name, PageQuery pageQuery);
}
