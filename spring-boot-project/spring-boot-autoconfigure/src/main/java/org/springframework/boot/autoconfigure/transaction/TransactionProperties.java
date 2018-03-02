/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.transaction;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Configuration properties that can be applied to an
 * {@link AbstractPlatformTransactionManager}.
 *
 * @author Kazuki Shimizu
 * @author Phillip Webb
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.transaction")
public class TransactionProperties implements
		PlatformTransactionManagerCustomizer<AbstractPlatformTransactionManager> {

	/**
	 * Default transaction timeout. If a duration suffix is not specified, seconds will be
	 * used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration defaultTimeout;

	/**
	 * Whether to roll back on commit failures.
	 */
	private Boolean rollbackOnCommitFailure;

	public Duration getDefaultTimeout() {
		return this.defaultTimeout;
	}

	public void setDefaultTimeout(Duration defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public Boolean getRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}

	public void setRollbackOnCommitFailure(Boolean rollbackOnCommitFailure) {
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
