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
package org.springframework.boot.actuate.cassandra;

import java.util.Collection;
import java.util.Objects;

import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.cql.ReactiveSessionCallback;
import org.springframework.util.Assert;

/**
 * A {@link ReactiveHealthIndicator} for Cassandra.
 *
 * @author Artsiom Yudovin
 * @author Tomasz Lelek
 * @since 2.1.0
 */
public class CassandraReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveCassandraOperations reactiveCassandraOperations;

	/**
	 * Create a new {@link CassandraHealthIndicator} instance.
	 * @param reactiveCassandraOperations the Cassandra operations
	 */
	public CassandraReactiveHealthIndicator(ReactiveCassandraOperations reactiveCassandraOperations) {
		super("Cassandra health check failed");
		Assert.notNull(reactiveCassandraOperations, "ReactiveCassandraOperations must not be null");
		this.reactiveCassandraOperations = reactiveCassandraOperations;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {

		return this.reactiveCassandraOperations.getReactiveCqlOperations().execute(extractMetadata()).single()
				.map((metadata) -> buildHealth(builder, metadata));
	}

	protected Health buildHealth(Builder builder, Metadata metadata) {
		Collection<Node> nodes = metadata.getNodes().values();
		boolean atLeastOneUp = nodes.stream().map(Node::getState).anyMatch((state) -> state == NodeState.UP);
		if (atLeastOneUp) {
			builder.up();
		}
		else {
			builder.down();
		}

		// fill details with version of the first node (if the version is not null)
		nodes.stream().map(Node::getCassandraVersion).filter(Objects::nonNull).findFirst()
				.ifPresent((version) -> builder.withDetail("version", version));
		return builder.build();
	}

	protected ReactiveSessionCallback<Metadata> extractMetadata() {
		return (session) -> Mono
				.fromSupplier(() -> ((DefaultDriverContext) session.getContext()).getMetadataManager().getMetadata());
	}

}
