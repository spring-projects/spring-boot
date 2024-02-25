/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.core.diagnostics.ClusterState;
import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.core.diagnostics.EndpointDiagnostics;

import org.springframework.boot.actuate.health.Health.Builder;

/**
 * Details of Couchbase's health.
 *
 * @author Andy Wilkinson
 */
class CouchbaseHealth {

	private final DiagnosticsResult diagnostics;

	/**
	 * Creates a new instance of CouchbaseHealth with the specified diagnostics result.
	 * @param diagnostics the diagnostics result to be associated with the CouchbaseHealth
	 * instance
	 */
	CouchbaseHealth(DiagnosticsResult diagnostics) {
		this.diagnostics = diagnostics;
	}

	/**
	 * Applies the Couchbase health status to the given builder.
	 * @param builder the builder to apply the health status to
	 */
	void applyTo(Builder builder) {
		builder = isCouchbaseUp(this.diagnostics) ? builder.up() : builder.down();
		builder.withDetail("sdk", this.diagnostics.sdk());
		builder.withDetail("endpoints",
				this.diagnostics.endpoints()
					.values()
					.stream()
					.flatMap(Collection::stream)
					.map(this::describe)
					.toList());
	}

	/**
	 * Checks if the Couchbase cluster is up and running.
	 * @param diagnostics the diagnostics result of the Couchbase cluster
	 * @return true if the Couchbase cluster is online, false otherwise
	 */
	private boolean isCouchbaseUp(DiagnosticsResult diagnostics) {
		return diagnostics.state() == ClusterState.ONLINE;
	}

	/**
	 * Generates a map containing the description of the given EndpointDiagnostics object.
	 * @param endpointHealth the EndpointDiagnostics object to describe
	 * @return a map containing the description of the EndpointDiagnostics object
	 */
	private Map<String, Object> describe(EndpointDiagnostics endpointHealth) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", endpointHealth.id());
		map.put("lastActivity", endpointHealth.lastActivity());
		map.put("local", endpointHealth.local());
		map.put("remote", endpointHealth.remote());
		map.put("state", endpointHealth.state());
		map.put("type", endpointHealth.type());
		return map;
	}

}
