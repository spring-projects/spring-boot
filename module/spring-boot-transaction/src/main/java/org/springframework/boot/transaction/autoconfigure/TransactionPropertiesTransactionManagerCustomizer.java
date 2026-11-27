/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * {@link TransactionManagerCustomizer} that set {@link TransactionProperties transaction
 * properties} to any transaction manager that is
 * {@link AbstractPlatformTransactionManager}.
 *
 * @author Yanming Zhou
 */
class TransactionPropertiesTransactionManagerCustomizer
		implements TransactionManagerCustomizer<AbstractPlatformTransactionManager> {

	private final TransactionProperties properties;

	TransactionPropertiesTransactionManagerCustomizer(TransactionProperties properties) {
		this.properties = properties;
	}

	@Override
	public void customize(AbstractPlatformTransactionManager transactionManager) {
		if (this.properties.getDefaultTimeout() != null) {
			transactionManager.setDefaultTimeout((int) this.properties.getDefaultTimeout().getSeconds());
		}
		if (this.properties.getRollbackOnCommitFailure() != null) {
			transactionManager.setRollbackOnCommitFailure(this.properties.getRollbackOnCommitFailure());
		}
	}

}
