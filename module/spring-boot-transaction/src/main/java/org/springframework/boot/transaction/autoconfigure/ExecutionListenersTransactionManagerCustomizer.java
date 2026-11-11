/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.util.List;

import org.springframework.transaction.ConfigurableTransactionManager;
import org.springframework.transaction.TransactionExecutionListener;

/**
 * {@link TransactionManagerCustomizer} that adds {@link TransactionExecutionListener
 * execution listeners} to any transaction manager that is
 * {@link ConfigurableTransactionManager configurable}.
 *
 * @author Andy Wilkinson
 */
class ExecutionListenersTransactionManagerCustomizer
		implements TransactionManagerCustomizer<ConfigurableTransactionManager> {

	private final List<TransactionExecutionListener> listeners;

	ExecutionListenersTransactionManagerCustomizer(List<TransactionExecutionListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void customize(ConfigurableTransactionManager transactionManager) {
		this.listeners.forEach(transactionManager::addListener);
	}

}
