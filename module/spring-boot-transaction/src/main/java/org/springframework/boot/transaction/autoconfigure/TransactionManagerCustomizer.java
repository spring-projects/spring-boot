/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import org.springframework.transaction.TransactionManager;

/**
 * Callback interface that can be implemented by beans wishing to customize a
 * {@link TransactionManager} to fine-tune its auto-configuration.
 *
 * @param <T> the transaction manager type
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface TransactionManagerCustomizer<T extends TransactionManager> {

	/**
	 * Customize the given transaction manager.
	 * @param transactionManager the transaction manager to customize
	 */
	void customize(T transactionManager);

}
