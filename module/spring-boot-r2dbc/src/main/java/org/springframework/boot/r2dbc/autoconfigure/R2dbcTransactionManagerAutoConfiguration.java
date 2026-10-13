/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link R2dbcTransactionManager}.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@ConditionalOnClass({ R2dbcTransactionManager.class, ReactiveTransactionManager.class })
@ConditionalOnSingleCandidate(ConnectionFactory.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public final class R2dbcTransactionManagerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveTransactionManager.class)
	R2dbcTransactionManager connectionFactoryTransactionManager(ConnectionFactory connectionFactory) {
		return new R2dbcTransactionManager(connectionFactory);
	}

}
