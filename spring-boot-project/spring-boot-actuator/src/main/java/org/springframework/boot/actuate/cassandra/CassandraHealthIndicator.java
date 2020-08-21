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
import com.datastax.oss.driver.api.core.session.Session;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Cassandra data stores.
 *
 * @author Julien Dubois
 * @author Alexandre Dutra
 * @author Tomasz Lelek
 * @since 2.0.0
 */
public class CassandraHealthIndicator extends AbstractHealthIndicator {

	private CassandraOperations cassandraOperations;

	public CassandraHealthIndicator() {
		super("Cassandra health check failed");
	}

	/**
	 * Create a new {@link CassandraHealthIndicator} instance.
	 * @param cassandraOperations the Cassandra operations
	 */
	public CassandraHealthIndicator(CassandraOperations cassandraOperations) {
		super("Cassandra health check failed");
		Assert.notNull(cassandraOperations, "CassandraOperations must not be null");
		this.cassandraOperations = cassandraOperations;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Metadata metadata = this.cassandraOperations.getCqlOperations().execute(Session::getMetadata);
		if (metadata == null) {
			throw new IllegalStateException("The CqlSession metadata was null; cannot perform health check.");
		}
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

	}

}
