package com.cocos.challenge.infrastructure.persistence;

import com.cocos.challenge.domain.repositories.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransactionRunnerAdapter implements TransactionRunner {

	private final TransactionTemplate transactions;

	public TransactionRunnerAdapter(PlatformTransactionManager transactionManager) {
		this.transactions = new TransactionTemplate(transactionManager);
	}

	@Override
	public <T> T run(Supplier<T> work) {
		return transactions.execute(status -> work.get());
	}
}
