package com.cocos.challenge.domain.repositories;

import com.cocos.challenge.domain.model.Quote;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface MarketDataRepository {

	Optional<Quote> findLatest(Long instrumentId);

	Map<Long, Quote> findLatestByInstrumentIds(Collection<Long> instrumentIds);
}
