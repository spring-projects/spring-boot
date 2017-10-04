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

package org.springframework.boot.actuate.system;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 */
public class DiskSpaceHealthIndicatorTests {

	static final long THRESHOLD_BYTES = 1024;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private File fileMock;

	private HealthIndicator healthIndicator;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.fileMock.exists()).willReturn(true);
		given(this.fileMock.canRead()).willReturn(true);
		this.healthIndicator = new DiskSpaceHealthIndicator(this.fileMock,
				THRESHOLD_BYTES);
	}

	@Test
	public void diskSpaceIsUp() throws Exception {
		given(this.fileMock.getUsableSpace()).willReturn(THRESHOLD_BYTES + 10);
		given(this.fileMock.getTotalSpace()).willReturn(THRESHOLD_BYTES * 10);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD_BYTES);
		assertThat(health.getDetails().get("free")).isEqualTo(THRESHOLD_BYTES + 10);
		assertThat(health.getDetails().get("total")).isEqualTo(THRESHOLD_BYTES * 10);
	}

	@Test
	public void diskSpaceIsDown() throws Exception {
		given(this.fileMock.getUsableSpace()).willReturn(THRESHOLD_BYTES - 10);
		given(this.fileMock.getTotalSpace()).willReturn(THRESHOLD_BYTES * 10);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("threshold")).isEqualTo(THRESHOLD_BYTES);
		assertThat(health.getDetails().get("free")).isEqualTo(THRESHOLD_BYTES - 10);
		assertThat(health.getDetails().get("total")).isEqualTo(THRESHOLD_BYTES * 10);
	}

}
