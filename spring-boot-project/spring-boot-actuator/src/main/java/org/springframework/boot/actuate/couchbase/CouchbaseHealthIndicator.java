/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.couchbase;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.core.message.internal.EndpointHealth;
import com.couchbase.client.core.state.LifecycleState;
import com.couchbase.client.java.Cluster;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CouchbaseHealthIndicator extends AbstractHealthIndicator {

	private final Cluster cluster;

	/**
	 * Create an indicator with the specified {@link Cluster}.
	 * @param cluster the Couchbase Cluster
	 * @since 2.0.6
	 */
	public CouchbaseHealthIndicator(Cluster cluster) {
		super("Couchbase health check failed");
		Assert.notNull(cluster, "Cluster must not be null");
		this.cluster = cluster;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		DiagnosticsReport diagnostics = this.cluster.diagnostics();
		builder = isCouchbaseUp(diagnostics) ? builder.up() : builder.down();
		builder.withDetail("sdk", diagnostics.sdk());
		builder.withDetail("endpoints", diagnostics.endpoints().stream()
				.map(this::describe).collect(Collectors.toList()));
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
