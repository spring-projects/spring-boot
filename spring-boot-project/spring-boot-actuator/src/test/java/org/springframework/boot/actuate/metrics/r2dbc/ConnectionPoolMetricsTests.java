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

package org.springframework.boot.actuate.metrics.r2dbc;

import java.util.Collections;
import java.util.UUID;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.h2.CloseableConnectionFactory;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2ConnectionOption;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionPoolMetrics}.
 *
 * @author Tadaya Tsuyukubo
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class ConnectionPoolMetricsTests {

	private static final Tag testTag = Tag.of("test", "yes");

	private static final Tag regionTag = Tag.of("region", "eu-2");

	private CloseableConnectionFactory connectionFactory;

	@BeforeEach
	void init() {
		this.connectionFactory = H2ConnectionFactory.inMemory("db-" + UUID.randomUUID(), "sa", "",
				Collections.singletonMap(H2ConnectionOption.DB_CLOSE_DELAY, "-1"));
	}

	@AfterEach
	void close() {
		if (this.connectionFactory != null) {
			this.connectionFactory.close();
		}
	}

	@Test
	void connectionFactoryIsInstrumented() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ConnectionPool connectionPool = new ConnectionPool(
				ConnectionPoolConfiguration.builder(this.connectionFactory).initialSize(3).maxSize(7).build());
		ConnectionPoolMetrics metrics = new ConnectionPoolMetrics(connectionPool, "test-pool",
				Tags.of(testTag, regionTag));
		metrics.bindTo(registry);
		// acquire two connections
		connectionPool.create().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		connectionPool.create().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		assertGauge(registry, "r2dbc.pool.acquired", 2);
		assertGauge(registry, "r2dbc.pool.allocated", 3);
		assertGauge(registry, "r2dbc.pool.idle", 1);
		assertGauge(registry, "r2dbc.pool.pending", 0);
		assertGauge(registry, "r2dbc.pool.max.allocated", 7);
		assertGauge(registry, "r2dbc.pool.max.pending", Integer.MAX_VALUE);
	}

	private void assertGauge(SimpleMeterRegistry registry, String metric, int expectedValue) {
		Gauge gauge = registry.get(metric).gauge();
		assertThat(gauge.value()).isEqualTo(expectedValue);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "test-pool"), testTag, regionTag);
	}

}
