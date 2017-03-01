/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.lang.management.MemoryUsage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MemoryHealthIndicator}.
 *
 * @author Lari Hotari
 */
public class MemoryHealthIndicatorTests {

	static final int THRESHOLD_PERCENTAGE = 90;

	@Mock
	private LowMemoryDetector lowMemoryDetectorMock;

	private HealthIndicator healthIndicator;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.lowMemoryDetectorMock.getOccupiedHeapPercentageThreshold())
				.willReturn(THRESHOLD_PERCENTAGE);
		this.healthIndicator = new MemoryHealthIndicator(this.lowMemoryDetectorMock);
	}

	@Test
	public void memoryIsHealthy() throws Exception {
		given(this.lowMemoryDetectorMock.isHealthy()).willReturn(true);
		MemoryUsage memoryUsage = createMemoryUsage(1000000L, 3000000L);
		given(this.lowMemoryDetectorMock.getCurrentUsage()).willReturn(memoryUsage);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("thresholdPercentage"))
				.isEqualTo(THRESHOLD_PERCENTAGE);
		assertThat(health.getDetails().get("used")).isEqualTo(1000000L);
		assertThat(health.getDetails().get("max")).isEqualTo(3000000L);
	}

	@Test
	public void memoryIsUnhealthy() throws Exception {
		given(this.lowMemoryDetectorMock.isHealthy()).willReturn(false);
		MemoryUsage memoryUsage = createMemoryUsage(2900000L, 3000000L);
		given(this.lowMemoryDetectorMock.getLowMemoryStateUsage()).willReturn(memoryUsage);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("thresholdPercentage"))
				.isEqualTo(THRESHOLD_PERCENTAGE);
		assertThat(health.getDetails().get("used")).isEqualTo(2900000L);
		assertThat(health.getDetails().get("max")).isEqualTo(3000000L);
	}

	private MemoryUsage createMemoryUsage(long usage, long usageMax) {
		MemoryUsage memoryUsage = mock(MemoryUsage.class);
		given(memoryUsage.getUsed()).willReturn(usage);
		given(memoryUsage.getMax()).willReturn(usageMax);
		return memoryUsage;
	}
}
