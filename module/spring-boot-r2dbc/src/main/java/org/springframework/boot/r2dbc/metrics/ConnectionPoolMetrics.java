/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Gauge.Builder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;

/**
 * A {@link MeterBinder} for a {@link ConnectionPool}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class ConnectionPoolMetrics implements MeterBinder {

	private static final String CONNECTIONS = "connections";

	private final ConnectionPool pool;

	private final Iterable<Tag> tags;

	public ConnectionPoolMetrics(ConnectionPool pool, String name, Iterable<Tag> tags) {
		this.pool = pool;
		this.tags = Tags.concat(tags, "name", name);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.pool.getMetrics().ifPresent((poolMetrics) -> {
			bindConnectionPoolMetric(registry,
					Gauge.builder(metricKey("acquired"), poolMetrics, PoolMetrics::acquiredSize)
						.description("Size of successfully acquired connections which are in active use."));
			bindConnectionPoolMetric(registry,
					Gauge.builder(metricKey("allocated"), poolMetrics, PoolMetrics::allocatedSize)
						.description("Size of allocated connections in the pool which are in active use or idle."));
			bindConnectionPoolMetric(registry, Gauge.builder(metricKey("idle"), poolMetrics, PoolMetrics::idleSize)
				.description("Size of idle connections in the pool."));
			bindConnectionPoolMetric(registry,
					Gauge.builder(metricKey("pending"), poolMetrics, PoolMetrics::pendingAcquireSize)
						.description("Size of pending to acquire connections from the underlying connection factory."));
			bindConnectionPoolMetric(registry,
					Gauge.builder(metricKey("max.allocated"), poolMetrics, PoolMetrics::getMaxAllocatedSize)
						.description("Maximum size of allocated connections that this pool allows."));
			bindConnectionPoolMetric(registry,
					Gauge.builder(metricKey("max.pending"), poolMetrics, PoolMetrics::getMaxPendingAcquireSize)
						.description("Maximum size of pending state to acquire connections that this pool allows."));
		});
	}

	private void bindConnectionPoolMetric(MeterRegistry registry, Builder<?> builder) {
		builder.tags(this.tags).baseUnit(CONNECTIONS).register(registry);
	}

	private static String metricKey(String name) {
		return "r2dbc.pool." + name;
	}

}
