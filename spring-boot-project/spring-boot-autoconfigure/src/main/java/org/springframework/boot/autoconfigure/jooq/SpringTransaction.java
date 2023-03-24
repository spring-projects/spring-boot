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

package org.springframework.boot.autoconfigure.jooq;

import org.jooq.Transaction;

import org.springframework.transaction.TransactionStatus;

/**
 * Adapts a Spring transaction for JOOQ.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 */
class SpringTransaction implements Transaction {

	// Based on the jOOQ-spring-example from https://github.com/jOOQ/jOOQ

	private final TransactionStatus transactionStatus;

	SpringTransaction(TransactionStatus transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	TransactionStatus getTxStatus() {
		return this.transactionStatus;
	}

}
