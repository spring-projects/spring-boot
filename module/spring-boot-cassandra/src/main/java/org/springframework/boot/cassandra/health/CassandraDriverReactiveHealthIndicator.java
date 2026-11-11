/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.health;

import java.util.Collection;
import java.util.Optional;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link ReactiveHealthIndicator} returning status information
 * for Cassandra data stores.
 *
 * @author Alexandre Dutra
 * @author Tomasz Lelek
 * @since 4.0.0
 */
public class CassandraDriverReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final CqlSession session;

	/**
	 * Create a new {@link CassandraDriverReactiveHealthIndicator} instance.
	 * @param session the {@link CqlSession}.
	 */
	public CassandraDriverReactiveHealthIndicator(CqlSession session) {
		super("Cassandra health check failed");
		Assert.notNull(session, "'session' must not be null");
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
