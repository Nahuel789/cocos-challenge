package com.cocos.challenge.infrastructure.persistence.adapter;

import com.cocos.challenge.domain.model.Quote;
import com.cocos.challenge.domain.repositories.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;
import com.cocos.challenge.infrastructure.persistence.mapper.Ids;
import com.cocos.challenge.infrastructure.persistence.mapper.PersistenceMapper;
import com.cocos.challenge.infrastructure.persistence.repository.SpringMarketDataRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MarketDataRepositoryAdapter implements MarketDataRepository {

	private final SpringMarketDataRepository marketData;

	public MarketDataRepositoryAdapter(SpringMarketDataRepository marketData) {
		this.marketData = marketData;
	}

	@Override
	public Optional<Quote> findLatest(Long instrumentId) {
		return Optional.ofNullable(findLatestByInstrumentIds(List.of(instrumentId)).get(instrumentId));
	}

	@Override
	public Map<Long, Quote> findLatestByInstrumentIds(Collection<Long> instrumentIds) {
		if (instrumentIds == null || instrumentIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Quote> quotes = new LinkedHashMap<>();
		for (MarketDataEntity row : marketData.findLatest(Ids.db(instrumentIds))) {
			Quote quote = PersistenceMapper.toDomain(row);
			quotes.putIfAbsent(quote.instrumentId(), quote);
		}
		return Map.copyOf(quotes);
	}
}
