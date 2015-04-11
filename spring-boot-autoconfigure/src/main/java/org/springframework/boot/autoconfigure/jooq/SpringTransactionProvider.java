/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Allows Spring Transaction to be used with JOOQ.
 *
 * @author Lukas Eder
 * @see <a
 * href="https://github.com/jOOQ/jOOQ/commit/03b4797a4548ff8e46fe3368e719ae0d14a9cd90#diff-031df6070c4380b5b04bbd7f83ff01cd">Original
 * source</a>
 */
class SpringTransactionProvider implements TransactionProvider {

	private final PlatformTransactionManager txManager;

	SpringTransactionProvider(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	@Override
	public void begin(TransactionContext ctx) {
		TransactionStatus txStatus = txManager
				.getTransaction(new DefaultTransactionDefinition(
						TransactionDefinition.PROPAGATION_NESTED));

		ctx.transaction(new SpringTransaction(txStatus));
	}

	@Override
	public void commit(TransactionContext ctx) {
		txManager.commit(((SpringTransaction) ctx.transaction()).getTxStatus());
	}

	@Override
	public void rollback(TransactionContext ctx) {
		txManager.rollback(((SpringTransaction) ctx.transaction()).getTxStatus());
	}
}
