/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.metrics.ConnectionPoolMetrics;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionFactory R2DBC connection factories}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ConnectionPool.class, MeterRegistry.class })
@ConditionalOnBean({ ConnectionFactory.class, MeterRegistry.class })
public final class ConnectionPoolMetricsAutoConfiguration {

	@Autowired
	void bindConnectionPoolsToRegistry(ConfigurableListableBeanFactory beanFactory, MeterRegistry registry) {
		SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory, ConnectionFactory.class)
			.forEach((beanName, connectionFactory) -> {
				ConnectionPool pool = extractPool(connectionFactory);
				if (pool != null) {
					new ConnectionPoolMetrics(pool, beanName, Tags.empty()).bindTo(registry);
				}
			});
	}

	private @Nullable ConnectionPool extractPool(Object candidate) {
		if (candidate instanceof ConnectionPool connectionPool) {
			return connectionPool;
		}
		if (candidate instanceof Wrapped) {
			return extractPool(((Wrapped<?>) candidate).unwrap());
		}
		return null;
	}

}
