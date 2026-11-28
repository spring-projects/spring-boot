/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.connection.ConnectionHolder;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;

/**
 * Retains connections while jOOQ releases its per-statement connection handles.
 *
 * @author George Papadopoulos
 */
final class JooqR2dbcTransactionManager extends R2dbcTransactionManager {

	JooqR2dbcTransactionManager(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction,
			TransactionDefinition definition) {
		return Mono.defer(() -> {
			// Check if this is a new transaction or not.
			// We have to invoke requested() only for new transactions
			boolean hadConnectionHolder = synchronizationManager.hasResource(obtainConnectionFactory());
			return super.doBegin(synchronizationManager, transaction, definition).doOnSuccess((unused) -> {
				if (!hadConnectionHolder) {
					ConnectionHolder holder = (ConnectionHolder) synchronizationManager
						.getResource(obtainConnectionFactory());
					if (holder != null) {
						/*
						 * Jooq invokes create()/close() through `ConnectionFactory` per
						 * statement, rather than once for the whole transaction. But the
						 * reference-counted `ConnectionHolder` is 0-based. That means
						 * after the first statement (before committing the transaction)
						 * ConnectionHolder.released() is being invoked, which effectively
						 * drops the connection due to us reaching 0 references.
						 *
						 * To fix this we call ourselves the requested() function, which
						 * makes the counter to start from 1. No matching released() is
						 * needed because doCleanupAfterCompletion() unbinds and closes
						 * the connection directly at transaction end, bypassing the
						 * reference count entirely.
						 */
						holder.requested();
					}
				}
			});
		});
	}

}
