/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.health;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

/**
 * A {@link ReactiveHealthIndicator} for Couchbase.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class CouchbaseReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final Cluster cluster;

	/**
	 * Create a new {@link CouchbaseReactiveHealthIndicator} instance.
	 * @param cluster the Couchbase cluster
	 */
	public CouchbaseReactiveHealthIndicator(Cluster cluster) {
		super("Couchbase health check failed");
		this.cluster = cluster;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.cluster.reactive().diagnostics().map((diagnostics) -> {
			new CouchbaseHealth(diagnostics).applyTo(builder);
			return builder.build();
		});
	}

}
