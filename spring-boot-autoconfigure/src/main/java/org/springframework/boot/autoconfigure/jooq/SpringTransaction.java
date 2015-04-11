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

import org.jooq.Transaction;
import org.springframework.transaction.TransactionStatus;

/**
 * Adapts a Spring transaction for JOOQ.
 *
 * @author Lukas Eder
 *
 * @see <a
 * href="https://github.com/jOOQ/jOOQ/commit/7940d705ac056317125fb61de4eb871d44c37144#diff-5c89ff190c177a4c2170e3f9648714cc">Original
 * source</a>
 */
class SpringTransaction implements Transaction {
	private final TransactionStatus txStatus;

	public SpringTransaction(TransactionStatus txStatus) {
		this.txStatus = txStatus;
	}

	public TransactionStatus getTxStatus() {
		return txStatus;
	}
}
