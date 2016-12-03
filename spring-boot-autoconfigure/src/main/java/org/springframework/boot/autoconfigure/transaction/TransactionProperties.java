/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * External configuration properties for a {@link org.springframework.transaction.PlatformTransactionManager} created by
 * Spring. All {@literal spring.transaction.} properties are also applied to the {@code PlatformTransactionManager}.
 *
 * @author Kazuki Shimizu
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.transaction")
public class TransactionProperties {

	/**
	 * The default transaction timeout (sec).
	 */
	private Integer defaultTimeout;

	/**
	 * The indicating flag whether perform the rollback processing on commit failure (If perform rollback, set to the true).
	 */
	private Boolean rollbackOnCommitFailure;

	public Integer getDefaultTimeout() {
		return this.defaultTimeout;
	}

	public void setDefaultTimeout(Integer defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public Boolean getRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}

	public void setRollbackOnCommitFailure(Boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	/**
	 * Apply all transaction custom properties to a specified {@link PlatformTransactionManager} instance.
	 *
	 * @param transactionManager the target transaction manager
	 * @see AbstractPlatformTransactionManager#setDefaultTimeout(int)
	 * @see AbstractPlatformTransactionManager#setRollbackOnCommitFailure(boolean)
	 */
	public void applyTo(PlatformTransactionManager transactionManager) {
		if (transactionManager instanceof AbstractPlatformTransactionManager) {
			AbstractPlatformTransactionManager abstractPlatformTransactionManager =
					(AbstractPlatformTransactionManager) transactionManager;
			if (this.defaultTimeout != null) {
				abstractPlatformTransactionManager.setDefaultTimeout(this.defaultTimeout);
			}
			if (this.rollbackOnCommitFailure != null) {
				abstractPlatformTransactionManager.setRollbackOnCommitFailure(this.rollbackOnCommitFailure);
			}
		}
	}

}
