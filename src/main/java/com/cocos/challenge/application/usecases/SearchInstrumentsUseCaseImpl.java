package com.cocos.challenge.application.usecases;

import com.cocos.challenge.domain.model.*;
import com.cocos.challenge.domain.repositories.InstrumentRepository;
import com.cocos.challenge.domain.repositories.MarketDataRepository;
import com.cocos.challenge.domain.usecases.SearchInstrumentsUseCase;

import java.util.List;
import java.util.Map;

public final class SearchInstrumentsUseCaseImpl implements SearchInstrumentsUseCase {

	private final InstrumentRepository instruments;
	private final MarketDataRepository marketData;

	public SearchInstrumentsUseCaseImpl(InstrumentRepository instruments, MarketDataRepository marketData) {
		this.instruments = instruments;
		this.marketData = marketData;
	}

	@Override
	public Page<InstrumentQuote> search(String ticker, String name, PageQuery pageQuery) {
		Page<Instrument> found = instruments.search(likeFragment(ticker), likeFragment(name), pageQuery);
		List<Long> ids = found.content().stream().map(Instrument::id).toList();
		Map<Long, Quote> quotes = marketData.findLatestByInstrumentIds(ids);
		return found.map(instrument -> toQuote(instrument, quotes.get(instrument.id())));
	}

	private static String likeFragment(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim()
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}

	private static InstrumentQuote toQuote(Instrument instrument, Quote quote) {
		return new InstrumentQuote(
				instrument.id(),
				instrument.ticker(),
				instrument.name(),
				instrument.type(),
				quote == null || quote.close() == null ? null : Decimals.price(quote.close()),
				quote == null ? null : quote.dailyReturnPercent());
	}
}
