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
import java.util.Optional;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link ReactiveHealthIndicator} returning status information
 * for Cassandra data stores.
 *
 * @author Alexandre Dutra
 * @author Tomasz Lelek
 * @since 2.4.0
 */
public class CassandraDriverReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final CqlSession session;

	/**
	 * Create a new {@link CassandraHealthIndicator} instance.
	 * @param session the {@link CqlSession}.
	 */
	public CassandraDriverReactiveHealthIndicator(CqlSession session) {
		super("Cassandra health check failed");
		Assert.notNull(session, "session must not be null");
		this.session = session;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return Mono.fromSupplier(() -> {
			Collection<Node> nodes = this.session.getMetadata().getNodes().values();
			Optional<Node> nodeUp = nodes.stream().filter((node) -> node.getState() == NodeState.UP).findAny();
			builder.status(nodeUp.isPresent() ? Status.UP : Status.DOWN);
			nodeUp.map(Node::getCassandraVersion).ifPresent((version) -> builder.withDetail("version", version));
			return builder.build();
		});
	}

}
