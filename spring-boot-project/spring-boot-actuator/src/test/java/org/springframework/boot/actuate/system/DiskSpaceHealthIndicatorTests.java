/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.system;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Stephane Nicoll
 */
public class DiskSpaceHealthIndicatorTests {

	private static final DataSize THRESHOLD = DataSize.ofKilobytes(1);

	private static final DataSize TOTAL_SPACE = DataSize.ofKilobytes(10);

	@Mock
	private File fileMock;

	private HealthIndicator healthIndicator;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		given(this.fileMock.exists()).willReturn(true);
		given(this.fileMock.canRead()).willReturn(true);
		this.healthIndicator = new DiskSpaceHealthIndicator(this.fileMock, THRESHOLD);
	}

	@Test
	public void diskSpaceIsUp() {
		long freeSpace = THRESHOLD.toBytes() + 10;
		given(this.fileMock.getUsableSpace()).willReturn(freeSpace);
		given(this.fileMock.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD.toBytes());
		assertThat(health.getDetails().get("free")).isEqualTo(freeSpace);
		assertThat(health.getDetails().get("total")).isEqualTo(TOTAL_SPACE.toBytes());
	}

	@Test
	public void diskSpaceIsDown() {
		long freeSpace = THRESHOLD.toBytes() - 10;
		given(this.fileMock.getUsableSpace()).willReturn(freeSpace);
		given(this.fileMock.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD.toBytes());
		assertThat(health.getDetails().get("free")).isEqualTo(freeSpace);
		assertThat(health.getDetails().get("total")).isEqualTo(TOTAL_SPACE.toBytes());
	}

}
