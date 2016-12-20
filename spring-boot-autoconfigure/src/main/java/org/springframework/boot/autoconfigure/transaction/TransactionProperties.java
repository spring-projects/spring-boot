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

import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Nested configuration properties that can be applied to an
 * {@link AbstractPlatformTransactionManager}.
 *
 * @author Kazuki Shimizu
 * @since 1.5.0
 */
public class TransactionProperties {

	/**
	 * Default transaction timeout in seconds.
	 */
	private Integer defaultTimeout;

	/**
	 * Perform the rollback on commit failurures.
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
	 * Apply all transaction custom properties to the specified transaction manager.
	 * @param transactionManager the target transaction manager
	 */
	public void applyTo(AbstractPlatformTransactionManager transactionManager) {
		if (this.defaultTimeout != null) {
			transactionManager.setDefaultTimeout(this.defaultTimeout);
		}
		if (this.rollbackOnCommitFailure != null) {
			transactionManager.setRollbackOnCommitFailure(this.rollbackOnCommitFailure);
		}
	}

}
