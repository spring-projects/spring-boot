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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.Version;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link CassandraDriverReactiveHealthIndicator}.
 *
 * @author Alexandre Dutra
 * @author Tomasz Lelek
 * @since 2.4.0
 */
class CassandraDriverReactiveHealthIndicatorTests {

	@Test
	void createWhenCqlSessionIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CassandraDriverReactiveHealthIndicator(null));
	}

	@ParameterizedTest
	@MethodSource
	void reportCassandraHealthCheck(Map<UUID, Node> nodes, Status expectedStatus) {
		CqlSession session = mock(CqlSession.class);
		Metadata metadata = mock(Metadata.class);
		when(session.getMetadata()).thenReturn(metadata);
		when(metadata.getNodes()).thenReturn(nodes);

		CassandraDriverReactiveHealthIndicator healthIndicator = new CassandraDriverReactiveHealthIndicator(session);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> assertThat(h.getStatus()).isEqualTo(expectedStatus))
				.verifyComplete();
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
	void addVersionToDetailsIfReportedNotNull() {
		CqlSession session = mock(CqlSession.class);
		Metadata metadata = mock(Metadata.class);
		when(session.getMetadata()).thenReturn(metadata);
		Node node = mock(Node.class);
		when(node.getState()).thenReturn(NodeState.UP);
		when(node.getCassandraVersion()).thenReturn(Version.V4_0_0);
		when(metadata.getNodes()).thenReturn(createNodes(node));

		CassandraDriverReactiveHealthIndicator healthIndicator = new CassandraDriverReactiveHealthIndicator(session);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo(Version.V4_0_0);
		}).verifyComplete();
	}

	@Test
	void doNotAddVersionToDetailsIfReportedNull() {
		CqlSession session = mock(CqlSession.class);
		Metadata metadata = mock(Metadata.class);
		when(session.getMetadata()).thenReturn(metadata);
		Node node = mock(Node.class);
		when(node.getState()).thenReturn(NodeState.UP);
		when(metadata.getNodes()).thenReturn(createNodes(node));

		CassandraDriverReactiveHealthIndicator healthIndicator = new CassandraDriverReactiveHealthIndicator(session);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails().get("version")).isNull();
		}).verifyComplete();
	}

	@Test
	void testCassandraIsDown() {
		CqlSession session = mock(CqlSession.class);
		given(session.getMetadata()).willThrow(new DriverTimeoutException("Test Exception"));

		CassandraDriverReactiveHealthIndicator cassandraReactiveHealthIndicator = new CassandraDriverReactiveHealthIndicator(
				session);
		Mono<Health> health = cassandraReactiveHealthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("error");
			assertThat(h.getDetails().get("error"))
					.isEqualTo(DriverTimeoutException.class.getName() + ": Test Exception");
		}).verifyComplete();
	}

	private static Map<UUID, Node> createNodes(Node... nodes) {
		Map<UUID, Node> nodesMap = new HashMap<>();
		for (Node n : nodes) {
			nodesMap.put(UUID.randomUUID(), n);
		}

		return nodesMap;
	}

}
