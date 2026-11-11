/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Configuration of the R2DBC infrastructure based on a {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DatabaseClient.class)
@ConditionalOnSingleCandidate(ConnectionFactory.class)
class ConnectionFactoryDependentConfiguration {

	@Bean
	@ConditionalOnMissingBean
	DatabaseClient r2dbcDatabaseClient(ConnectionFactory connectionFactory) {
		return DatabaseClient.builder().connectionFactory(connectionFactory).build();
	}

}
