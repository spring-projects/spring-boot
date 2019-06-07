/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.couchbase;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.core.message.internal.EndpointHealth;
import com.couchbase.client.core.state.LifecycleState;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.cluster.ClusterInfo;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HealthIndicator} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CouchbaseHealthIndicator extends AbstractHealthIndicator {

	private final HealthCheck healthCheck;

	/**
	 * Create an indicator with the specified {@link CouchbaseOperations} and
	 * {@code timeout}.
	 * @param couchbaseOperations the couchbase operations
	 * @param timeout the request timeout
	 * @deprecated since 2.0.6 in favour of {@link #CouchbaseHealthIndicator(Cluster)}
	 */
	@Deprecated
	public CouchbaseHealthIndicator(CouchbaseOperations couchbaseOperations, Duration timeout) {
		super("Couchbase health check failed");
		Assert.notNull(couchbaseOperations, "CouchbaseOperations must not be null");
		Assert.notNull(timeout, "Timeout must not be null");
		this.healthCheck = new OperationsHealthCheck(couchbaseOperations, timeout.toMillis());
	}

	/**
	 * Create an indicator with the specified {@link CouchbaseOperations}.
	 * @param couchbaseOperations the couchbase operations
	 * @deprecated as of 2.0.5 in favour of
	 * {@link #CouchbaseHealthIndicator(CouchbaseOperations, Duration)}
	 */
	@Deprecated
	public CouchbaseHealthIndicator(CouchbaseOperations couchbaseOperations) {
		this(couchbaseOperations, Duration.ofSeconds(1));
	}

	/**
	 * Create an indicator with the specified {@link Cluster}.
	 * @param cluster the Couchbase Cluster
	 * @since 2.0.6
	 */
	public CouchbaseHealthIndicator(Cluster cluster) {
		super("Couchbase health check failed");
		Assert.notNull(cluster, "Cluster must not be null");
		this.healthCheck = new ClusterHealthCheck(cluster);
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		this.healthCheck.checkHealth(builder);
	}

	private interface HealthCheck {

		void checkHealth(Builder builder) throws Exception;

	}

	private static final class OperationsHealthCheck implements HealthCheck {

		private final CouchbaseOperations operations;

		private final long timeout;

		OperationsHealthCheck(CouchbaseOperations operations, long timeout) {
			this.operations = operations;
			this.timeout = timeout;
		}

		@Override
		public void checkHealth(Builder builder) throws Exception {
			ClusterInfo cluster = this.operations.getCouchbaseClusterInfo();
			BucketInfo bucket = getBucketInfo();
			String versions = StringUtils.collectionToCommaDelimitedString(cluster.getAllVersions());
			String nodes = StringUtils.collectionToCommaDelimitedString(bucket.nodeList());
			builder.up().withDetail("versions", versions).withDetail("nodes", nodes);
		}

		private BucketInfo getBucketInfo() throws Exception {
			try {
				return this.operations.getCouchbaseBucket().bucketManager().info(this.timeout, TimeUnit.MILLISECONDS);
			}
			catch (RuntimeException ex) {
				if (ex.getCause() instanceof TimeoutException) {
					throw (TimeoutException) ex.getCause();
				}
				throw ex;
			}
		}

	}

	private static class ClusterHealthCheck implements HealthCheck {

		private final Cluster cluster;

		ClusterHealthCheck(Cluster cluster) {
			this.cluster = cluster;
		}

		@Override
		public void checkHealth(Builder builder) throws Exception {
			DiagnosticsReport diagnostics = this.cluster.diagnostics();
			builder = isCouchbaseUp(diagnostics) ? builder.up() : builder.down();
			builder.withDetail("sdk", diagnostics.sdk());
			builder.withDetail("endpoints",
					diagnostics.endpoints().stream().map(this::describe).collect(Collectors.toList()));
		}

		private boolean isCouchbaseUp(DiagnosticsReport diagnostics) {
			for (EndpointHealth health : diagnostics.endpoints()) {
				LifecycleState state = health.state();
				if (state != LifecycleState.CONNECTED && state != LifecycleState.IDLE) {
					return false;
				}
			}
			return true;
		}

		private Map<String, Object> describe(EndpointHealth endpointHealth) {
			Map<String, Object> map = new HashMap<>();
			map.put("id", endpointHealth.id());
			map.put("lastActivity", endpointHealth.lastActivity());
			map.put("local", endpointHealth.local().toString());
			map.put("remote", endpointHealth.remote().toString());
			map.put("state", endpointHealth.state());
			map.put("type", endpointHealth.type());
			return map;
		}

	}

}
