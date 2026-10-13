/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.jspecify.annotations.Nullable;

import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;

/**
 * Generic Hibernate {@link AbstractJtaPlatform} implementation that simply resolves the
 * JTA {@link UserTransaction} and {@link TransactionManager} from the Spring-configured
 * {@link JtaTransactionManager} implementation.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 4.0.0
 */
public class SpringJtaPlatform extends AbstractJtaPlatform {

	private static final long serialVersionUID = 1L;

	private final JtaTransactionManager transactionManager;

	public SpringJtaPlatform(JtaTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "'transactionManager' must not be null");
		this.transactionManager = transactionManager;
	}

	@Override
	protected @Nullable TransactionManager locateTransactionManager() {
		return this.transactionManager.getTransactionManager();
	}

	@Override
	protected @Nullable UserTransaction locateUserTransaction() {
		return this.transactionManager.getUserTransaction();
	}

}
