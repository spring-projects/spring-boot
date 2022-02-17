/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.orm.jpa.hibernate;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;

import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;

/**
 * Generic Hibernate {@link AbstractJtaPlatform} implementation that simply resolves the
 * JTA {@link UserTransaction} and {@link TransactionManager} from the Spring-configured
 * {@link JtaTransactionManager} implementation.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
public class SpringJtaPlatform extends AbstractJtaPlatform {

	private static final long serialVersionUID = 1L;

	private final JtaTransactionManager transactionManager;

	public SpringJtaPlatform(JtaTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return this.transactionManager.getTransactionManager();
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return this.transactionManager.getUserTransaction();
	}

}
