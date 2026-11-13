package com.cocos.challenge.domain.usecases;

import com.cocos.challenge.domain.model.InstrumentQuote;
import com.cocos.challenge.domain.model.Page;
import com.cocos.challenge.domain.model.PageQuery;

public interface SearchInstrumentsUseCase {

	Page<InstrumentQuote> search(String ticker, String name, PageQuery pageQuery);
}
