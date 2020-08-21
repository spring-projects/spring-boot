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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.datastax.oss.driver.api.core.Version;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.cassandra.CassandraInternalException;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.data.cassandra.core.cql.SessionCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link CassandraHealthIndicator}.
 *
 * @author Oleksii Bondar
 * @author Stephane Nicoll
 * @author Tomasz Lelek
 */
class CassandraHealthIndicatorTests {

	@Test
	void createWhenCqlSessionIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CassandraHealthIndicator(null));
	}

	@ParameterizedTest
	@MethodSource
	@SuppressWarnings("unchecked")
	void reportCassandraHealthCheck(Map<UUID, Node> nodes, Status expectedStatus) {
		Metadata metadata = mock(Metadata.class);
		when(metadata.getNodes()).thenReturn(nodes);
		CqlOperations cqlOperations = mock(CqlOperations.class);
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		when(cqlOperations.execute(any(SessionCallback.class))).thenReturn(metadata);

		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(expectedStatus);
	}

	static Stream<Arguments> reportCassandraHealthCheck() {
		Node healthyNode = mock(Node.class);
		when(healthyNode.getState()).thenReturn(NodeState.UP);
		Node unhealthyNode = mock(Node.class);
		when(unhealthyNode.getState()).thenReturn(NodeState.DOWN);
		Node unknownNode = mock(Node.class);
		when(unknownNode.getState()).thenReturn(NodeState.UNKNOWN);
		Node forcedDownNode = mock(Node.class);
		when(forcedDownNode.getState()).thenReturn(NodeState.FORCED_DOWN);
		return Stream.<Arguments>builder().add(Arguments.arguments(createNodes(healthyNode), Status.UP))
				.add(Arguments.arguments(createNodes(unhealthyNode), Status.DOWN))
				.add(Arguments.arguments(createNodes(unknownNode), Status.DOWN))
				.add(Arguments.arguments(createNodes(forcedDownNode), Status.DOWN))
				.add(Arguments.arguments(createNodes(healthyNode, unhealthyNode), Status.UP))
				.add(Arguments.arguments(createNodes(healthyNode, unknownNode), Status.UP))
				.add(Arguments.arguments(createNodes(healthyNode, forcedDownNode), Status.UP)).build();
	}

	@Test
	@SuppressWarnings("unchecked")
	void addVersionToDetailsIfReportedNotNull() {
		Metadata metadata = mock(Metadata.class);
		Node node = mock(Node.class);
		when(node.getState()).thenReturn(NodeState.UP);
		when(node.getCassandraVersion()).thenReturn(Version.V4_0_0);
		when(metadata.getNodes()).thenReturn(createNodes(node));

		CqlOperations cqlOperations = mock(CqlOperations.class);
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		when(cqlOperations.execute(any(SessionCallback.class))).thenReturn(metadata);

		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo(Version.V4_0_0);
	}

	@Test
	@SuppressWarnings("unchecked")
	void doNotAddVersionToDetailsIfReportedNull() {
		Metadata metadata = mock(Metadata.class);
		Node node = mock(Node.class);
		when(node.getState()).thenReturn(NodeState.UP);
		when(metadata.getNodes()).thenReturn(createNodes(node));

		CqlOperations cqlOperations = mock(CqlOperations.class);
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		given(cassandraOperations.getCqlOperations()).willReturn(cqlOperations);
		when(cqlOperations.execute(any(SessionCallback.class))).thenReturn(metadata);

		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isNull();
	}

	@Test
	void healthWithCassandraDown() {
		CassandraOperations cassandraOperations = mock(CassandraOperations.class);
		given(cassandraOperations.getCqlOperations()).willThrow(new CassandraInternalException("Connection failed"));
		CassandraHealthIndicator healthIndicator = new CassandraHealthIndicator(cassandraOperations);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error"))
				.isEqualTo(CassandraInternalException.class.getName() + ": Connection failed");
	}

	private static Map<UUID, Node> createNodes(Node... nodes) {
		Map<UUID, Node> nodesMap = new HashMap<>();
		for (Node n : nodes) {
			nodesMap.put(UUID.randomUUID(), n);
		}

		return nodesMap;
	}

}
