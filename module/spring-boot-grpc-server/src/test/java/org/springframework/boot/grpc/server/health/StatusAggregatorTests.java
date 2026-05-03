/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatusAggregator}.
 *
 * @author Phillip Webb
 */
class StatusAggregatorTests {

	@Test
	void ofAndGetAggregateStatusWhenUsingDefaultInstance() {
		StatusAggregator aggregator = StatusAggregator.getDefault();
		Status status = aggregator.getAggregateStatus(Status.DOWN, Status.UP, Status.UNKNOWN, Status.OUT_OF_SERVICE);
		assertThat(status).isEqualTo(Status.DOWN);
	}

	@Test
	void ofAndGetAggregateStatusWhenUsingCustomOrder() {
		StatusAggregator aggregator = StatusAggregator.of(Status.UNKNOWN, Status.UP, Status.OUT_OF_SERVICE,
				Status.DOWN);
		Status status = aggregator.getAggregateStatus(Status.DOWN, Status.UP, Status.UNKNOWN, Status.OUT_OF_SERVICE);
		assertThat(status).isEqualTo(Status.UNKNOWN);
	}

	@Test
	void ofAndGetAggregateStatusWhenHasCustomStatusAndUsingDefaultOrder() {
		StatusAggregator aggregator = StatusAggregator.getDefault();
		Status status = aggregator.getAggregateStatus(Status.DOWN, Status.UP, Status.UNKNOWN, Status.OUT_OF_SERVICE,
				new Status("CUSTOM"));
		assertThat(status).isEqualTo(Status.DOWN);
	}

	@Test
	void ofAndGetAggregateStatusWhenHasCustomStatusAndUsingCustomOrder() {
		StatusAggregator aggregator = StatusAggregator.of("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN", "CUSTOM");
		Status status = aggregator.getAggregateStatus(Status.DOWN, Status.UP, Status.UNKNOWN, Status.OUT_OF_SERVICE,
				new Status("CUSTOM"));
		assertThat(status).isEqualTo(Status.DOWN);
	}

	@Test
	void ofWithNonUniformCodes() {
		StatusAggregator aggregator = StatusAggregator.of("out-of-service", "up");
		Status status = aggregator.getAggregateStatus(Status.UP, Status.OUT_OF_SERVICE);
		assertThat(status).isEqualTo(Status.OUT_OF_SERVICE);
	}

}
