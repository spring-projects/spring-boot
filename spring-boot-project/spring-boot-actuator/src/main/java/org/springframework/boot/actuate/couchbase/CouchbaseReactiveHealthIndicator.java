/*
 * Copyright 2012-2020 the original author or authors.
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

import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;

/**
 * A {@link ReactiveHealthIndicator} for Couchbase.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 2.1.0
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
		DiagnosticsResult diagnostics = this.cluster.diagnostics();
		new CouchbaseHealth(diagnostics).applyTo(builder);
		return Mono.just(builder.build());
	}

}
