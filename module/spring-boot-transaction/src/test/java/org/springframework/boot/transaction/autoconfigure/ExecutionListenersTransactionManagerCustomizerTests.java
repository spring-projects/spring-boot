/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.transaction.ConfigurableTransactionManager;
import org.springframework.transaction.TransactionExecutionListener;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ExecutionListenersTransactionManagerCustomizer}.
 *
 * @author Andy Wilkinson
 */
class ExecutionListenersTransactionManagerCustomizerTests {

	@Test
	void whenTransactionManagerIsCustomizedThenExecutionListenersAreAddedToIt() {
		TransactionExecutionListener listener1 = mock(TransactionExecutionListener.class);
		TransactionExecutionListener listener2 = mock(TransactionExecutionListener.class);
		ConfigurableTransactionManager transactionManager = mock(ConfigurableTransactionManager.class);
		new ExecutionListenersTransactionManagerCustomizer(List.of(listener1, listener2)).customize(transactionManager);
		then(transactionManager).should().addListener(listener1);
		then(transactionManager).should().addListener(listener2);
	}

}
