/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.health;

import java.util.Collection;
import java.util.Optional;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Cassandra data stores.
 *
 * @author Alexandre Dutra
 * @author Tomasz Lelek
 * @since 4.0.0
 */
public class CassandraDriverHealthIndicator extends AbstractHealthIndicator {

	private final CqlSession session;

	/**
	 * Create a new {@link CassandraDriverHealthIndicator} instance.
	 * @param session the {@link CqlSession}.
	 */
	public CassandraDriverHealthIndicator(CqlSession session) {
		super("Cassandra health check failed");
		Assert.notNull(session, "'session' must not be null");
		this.session = session;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Collection<Node> nodes = this.session.getMetadata().getNodes().values();
		Optional<Node> nodeUp = nodes.stream().filter((node) -> node.getState() == NodeState.UP).findAny();
		builder.status(nodeUp.isPresent() ? Status.UP : Status.DOWN);
		nodeUp.map(Node::getCassandraVersion).ifPresent((version) -> builder.withDetail("version", version));
	}

}
