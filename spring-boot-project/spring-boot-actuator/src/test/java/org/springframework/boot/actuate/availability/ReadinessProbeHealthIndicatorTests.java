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

package org.springframework.boot.actuate.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailabilityProvider;
import org.springframework.boot.availability.ReadinessState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReadinessProbeHealthIndicator}
 *
 * @author Brian Clozel
 */
class ReadinessProbeHealthIndicatorTests {

	private ApplicationAvailabilityProvider stateProvider;

	private ReadinessProbeHealthIndicator healthIndicator;

	@BeforeEach
	void setUp() {
		this.stateProvider = mock(ApplicationAvailabilityProvider.class);
		this.healthIndicator = new ReadinessProbeHealthIndicator(this.stateProvider);
	}

	@Test
	void readinessIsReady() {
		when(this.stateProvider.getReadinessState()).thenReturn(ReadinessState.READY);
		assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void readinessIsUnready() {
		when(this.stateProvider.getReadinessState()).thenReturn(ReadinessState.UNREADY);
		assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
	}

}
