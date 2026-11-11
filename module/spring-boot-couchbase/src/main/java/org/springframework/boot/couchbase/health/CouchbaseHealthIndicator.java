/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.health;

import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.java.Cluster;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class CouchbaseHealthIndicator extends AbstractHealthIndicator {

	private final Cluster cluster;

	/**
	 * Create an indicator with the specified {@link Cluster}.
	 * @param cluster the Couchbase Cluster
	 */
	public CouchbaseHealthIndicator(Cluster cluster) {
		super("Couchbase health check failed");
		Assert.notNull(cluster, "'cluster' must not be null");
		this.cluster = cluster;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		DiagnosticsResult diagnostics = this.cluster.diagnostics();
		new CouchbaseHealth(diagnostics).applyTo(builder);
	}

}
