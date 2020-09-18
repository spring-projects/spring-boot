/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.metrics.cassandra;

import java.time.Duration;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraMetricsAutoConfiguration}.
 *
 * @author Erik Merkle
 */
@Testcontainers(disabledWithoutDocker = true)
public class CassandraMetricsAutoConfigurationIntegrationTests {

	@Container
	static final CassandraContainer<?> cassandra = new CassandraContainer<>().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10)).waitingFor(new CassandraQueryWaitStrategy());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(
					AutoConfigurations.of(CassandraMetricsAutoConfiguration.class, CassandraAutoConfiguration.class))
			.withPropertyValues(
					"spring.data.cassandra.contact-points:" + cassandra.getHost() + ":"
							+ cassandra.getFirstMappedPort(),
					"spring.data.cassandra.local-datacenter=datacenter1", "spring.data.cassandra.read-timeout=20s",
					"spring.data.cassandra.connect-timeout=10s");

	/**
	 * Cassandra driver metrics should be enabled by default as long as the desired
	 * metrics are enabled in the Driver's configuration.
	 */
	@Test
	void autoConfiguredCassandraIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			// execute queries to peg metrics
			CqlSession session = context.getBean(CqlSession.class);
			SimpleStatement statement = SimpleStatement.newInstance("SELECT release_version FROM system.local")
					.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
			for (int i = 0; i < 10; ++i) {
				assertThat(session.execute(statement).one()).isNotNull();
			}
			// assert Session metrics
			String sessionMetricPrefix = session.getContext().getSessionName() + ".";
			assertThat(
					registry.get(sessionMetricPrefix + DefaultSessionMetric.CONNECTED_NODES.getPath()).gauge().value())
							.isEqualTo(1d);
			assertThat(registry.get(sessionMetricPrefix + DefaultSessionMetric.CQL_REQUESTS.getPath()).timer().count())
					.isEqualTo(10L);
			assertThat(registry.get(sessionMetricPrefix + DefaultSessionMetric.BYTES_SENT.getPath()).counter().count())
					.isGreaterThan(1d);
			assertThat(
					registry.get(sessionMetricPrefix + DefaultSessionMetric.BYTES_RECEIVED.getPath()).counter().count())
							.isGreaterThan(1d);
			// assert Node metrics
			String nodeMetricPrefix = sessionMetricPrefix + "nodes." + cassandra.getHost() + ":"
					+ cassandra.getMappedPort(9042) + ".";
			assertThat(registry.get(nodeMetricPrefix + DefaultNodeMetric.BYTES_SENT.getPath()).counter().count())
					.isGreaterThan(1d);

		});
	}

}
