/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Configuration properties that can be applied to an
 * {@link AbstractPlatformTransactionManager}.
 *
 * @author Kazuki Shimizu
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.transaction")
public class TransactionProperties implements TransactionManagerCustomizer<AbstractPlatformTransactionManager> {

	/**
	 * Default transaction timeout. If a duration suffix is not specified, seconds will be
	 * used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private @Nullable Duration defaultTimeout;

	/**
	 * Whether to roll back on commit failures.
	 */
	private @Nullable Boolean rollbackOnCommitFailure;

	public @Nullable Duration getDefaultTimeout() {
		return this.defaultTimeout;
	}

	public void setDefaultTimeout(@Nullable Duration defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public @Nullable Boolean getRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}

	public void setRollbackOnCommitFailure(@Nullable Boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	@Override
	public void customize(AbstractPlatformTransactionManager transactionManager) {
		if (this.defaultTimeout != null) {
			transactionManager.setDefaultTimeout((int) this.defaultTimeout.getSeconds());
		}
		if (this.rollbackOnCommitFailure != null) {
			transactionManager.setRollbackOnCommitFailure(this.rollbackOnCommitFailure);
		}
	}

}
