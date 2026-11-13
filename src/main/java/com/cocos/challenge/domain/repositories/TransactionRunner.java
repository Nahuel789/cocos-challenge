package com.cocos.challenge.domain.repositories;

import java.util.function.Supplier;

public interface TransactionRunner {

	<T> T run(Supplier<T> work);
}
