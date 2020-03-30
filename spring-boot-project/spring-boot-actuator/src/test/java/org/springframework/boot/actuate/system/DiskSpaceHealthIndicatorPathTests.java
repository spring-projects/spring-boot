/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.system;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for the {@link DiskSpaceHealthIndicator} {@code path} parameter.
 *
 * @author Andreas Born
 */
class DiskSpaceHealthIndicatorPathTests {

	private static final DataSize THRESHOLD = DataSize.ofKilobytes(1);

	private static final DataSize TOTAL_SPACE = DataSize.ofKilobytes(10);

	@Mock
	private File fileMock;

	private HealthIndicator healthIndicator;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		given(this.fileMock.exists()).willReturn(false);
		given(this.fileMock.canRead()).willReturn(false);
		given(this.fileMock.canWrite()).willReturn(false);
		given(this.fileMock.canExecute()).willReturn(false);
		this.healthIndicator = new DiskSpaceHealthIndicator(this.fileMock, THRESHOLD);
	}

	@Test
	void diskSpaceIsDown() {
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD.toBytes());
		assertThat(health.getDetails().get("free")).isEqualTo(0L);
		assertThat(health.getDetails().get("total")).isEqualTo(0L);
		assertThat(health.getDetails().get("exists")).isEqualTo(false);
		assertThat(health.getDetails().get("canRead")).isEqualTo(false);
		assertThat(health.getDetails().get("canWrite")).isEqualTo(false);
		assertThat(health.getDetails().get("canExecute")).isEqualTo(false);
	}

	@Test
	void diskSpaceIsUpWhenPathOnlyExists() {
		long freeSpace = THRESHOLD.toBytes() + 10;
		given(this.fileMock.getUsableSpace()).willReturn(freeSpace);
		given(this.fileMock.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		given(this.fileMock.exists()).willReturn(true);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD.toBytes());
		assertThat(health.getDetails().get("free")).isEqualTo(freeSpace);
		assertThat(health.getDetails().get("total")).isEqualTo(TOTAL_SPACE.toBytes());
		assertThat(health.getDetails().get("exists")).isEqualTo(true);
		assertThat(health.getDetails().get("canRead")).isEqualTo(false);
		assertThat(health.getDetails().get("canWrite")).isEqualTo(false);
		assertThat(health.getDetails().get("canExecute")).isEqualTo(false);
	}

}
