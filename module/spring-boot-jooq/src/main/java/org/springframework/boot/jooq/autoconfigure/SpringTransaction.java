/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.jooq.Transaction;

import org.springframework.transaction.TransactionStatus;

/**
 * Adapts a Spring transaction for jOOQ.
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
