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

package org.springframework.boot.actuate.neo4j;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import org.neo4j.driver.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.actuate.neo4j.Neo4jDriverMetrics.PREFIX;
import static org.springframework.boot.actuate.neo4j.Neo4jDriverMocks.mockDriverWithMetrics;
import static org.springframework.boot.actuate.neo4j.Neo4jDriverMocks.mockDriverWithoutMetrics;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverMetricsTests {

	@Test
	void shouldDetectEnabledMetrics() {

		Driver driver = mockDriverWithMetrics();
		assertThat(Neo4jDriverMetrics.metricsAreEnabled(driver)).isTrue();
	}

	@Test
	void shouldDetectDisabledMetrics() {

		Driver driver = mockDriverWithoutMetrics();
		assertThat(Neo4jDriverMetrics.metricsAreEnabled(driver)).isFalse();
	}

	@Test
	void shouldRegisterCorrectMeters() {

		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		Neo4jDriverMetrics metrics = new Neo4jDriverMetrics("driver", mockDriverWithMetrics(), Collections.emptyList());
		metrics.bindTo(registry);

		assertThat(registry.get(PREFIX + ".acquired").functionCounter()).isNotNull();
		assertThat(registry.get(PREFIX + ".closed").functionCounter()).isNotNull();
		assertThat(registry.get(PREFIX + ".created").functionCounter()).isNotNull();
		assertThat(registry.get(PREFIX + ".failedToCreate").functionCounter()).isNotNull();
		assertThat(registry.get(PREFIX + ".idle").gauge()).isNotNull();
		assertThat(registry.get(PREFIX + ".inUse").gauge()).isNotNull();
		assertThat(registry.get(PREFIX + ".timedOutToAcquire").functionCounter()).isNotNull();
	}

}
