package com.cocos.challenge.domain.usecases;

import com.cocos.challenge.domain.model.Portfolio;

public interface GetPortfolioUseCase {

	Portfolio get(Long userId);
}
