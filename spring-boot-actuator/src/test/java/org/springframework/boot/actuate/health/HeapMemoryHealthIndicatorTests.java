/*
 * Copyright 2016-2016 the original author or authors.
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

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link HeapMemoryHeathIndicator}.
 *
 * @author Ben Hale
 */
public final class HeapMemoryHealthIndicatorTests {

	private static final double THRESHOLD = 0.5;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private MemoryMXBean memoryMXBeanMock;

	@Mock
	private MemoryUsage memoryUsageMock;

	private HealthIndicator healthIndicator;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.memoryMXBeanMock.getHeapMemoryUsage()).willReturn(this.memoryUsageMock);
		this.healthIndicator = new HeapMemoryHealthIndicator(
			createProperties(this.memoryMXBeanMock, THRESHOLD));
	}

	@Test
	public void heapMemoryIsUp() {
		given(this.memoryUsageMock.getInit()).willReturn(1L);
		given(this.memoryUsageMock.getUsed()).willReturn(2L);
		given(this.memoryUsageMock.getCommitted()).willReturn(3L);
		given(this.memoryUsageMock.getMax()).willReturn(6L);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("init")).isEqualTo(1L);
		assertThat(health.getDetails().get("used")).isEqualTo(2L);
		assertThat(health.getDetails().get("committed")).isEqualTo(3L);
		assertThat(health.getDetails().get("max")).isEqualTo(6L);
		assertThat(health.getDetails().get("threshold")).isEqualTo(3L);
	}

	@Test
	public void heapMemoryIsUpNoMax() {
		given(this.memoryUsageMock.getInit()).willReturn(1L);
		given(this.memoryUsageMock.getUsed()).willReturn(2L);
		given(this.memoryUsageMock.getCommitted()).willReturn(3L);
		given(this.memoryUsageMock.getMax()).willReturn(-1L);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("init")).isEqualTo(1L);
		assertThat(health.getDetails().get("used")).isEqualTo(2L);
		assertThat(health.getDetails().get("committed")).isEqualTo(3L);
		assertThat(health.getDetails().get("max")).isEqualTo(-1L);
		assertThat(health.getDetails().get("threshold")).isEqualTo(-1L);
	}

	@Test
	public void heapMemoryIsDown() {
		given(this.memoryUsageMock.getInit()).willReturn(1L);
		given(this.memoryUsageMock.getUsed()).willReturn(2L);
		given(this.memoryUsageMock.getCommitted()).willReturn(3L);
		given(this.memoryUsageMock.getMax()).willReturn(4L);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("init")).isEqualTo(1L);
		assertThat(health.getDetails().get("used")).isEqualTo(2L);
		assertThat(health.getDetails().get("committed")).isEqualTo(3L);
		assertThat(health.getDetails().get("max")).isEqualTo(4L);
		assertThat(health.getDetails().get("threshold")).isEqualTo(2L);
	}

	private HeapMemoryHealthIndicatorProperties createProperties(MemoryMXBean memoryMXBean, double threshold) {
		HeapMemoryHealthIndicatorProperties properties = new HeapMemoryHealthIndicatorProperties();
		properties.setMemoryMXBean(memoryMXBean);
		properties.setThreshold(threshold);
		return properties;
	}

}
