/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

/**
 * Allows Spring Transaction to be used with jOOQ.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @since 4.0.0
 */
public class SpringTransactionProvider implements TransactionProvider {

	// Based on the jOOQ-spring-example from https://github.com/jOOQ/jOOQ

	private final PlatformTransactionManager transactionManager;

	public SpringTransactionProvider(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void begin(TransactionContext context) {
		TransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);
		TransactionStatus status = this.transactionManager.getTransaction(definition);
		context.transaction(new SpringTransaction(status));
	}

	@Override
	public void commit(TransactionContext ctx) {
		this.transactionManager.commit(getTransactionStatus(ctx));
	}

	@Override
	public void rollback(TransactionContext ctx) {
		this.transactionManager.rollback(getTransactionStatus(ctx));
	}

	private TransactionStatus getTransactionStatus(TransactionContext ctx) {
		SpringTransaction transaction = (SpringTransaction) ctx.transaction();
		Assert.state(transaction != null, "'transaction' must not be null");
		return transaction.getTxStatus();
	}

}
