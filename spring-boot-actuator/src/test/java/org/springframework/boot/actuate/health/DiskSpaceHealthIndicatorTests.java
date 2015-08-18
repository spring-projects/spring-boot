/*
 * Copyright 2014-2015 the original author or authors.
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

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 */
@RunWith(MockitoJUnitRunner.class)
public class DiskSpaceHealthIndicatorTests {

	static final long THRESHOLD_BYTES = 1024;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	private File fileMock;

	private HealthIndicator healthIndicator;

	@Before
	public void setUp() throws Exception {
		given(this.fileMock.exists()).willReturn(true);
		given(this.fileMock.canRead()).willReturn(true);
		this.healthIndicator = new DiskSpaceHealthIndicator(createProperties(
				this.fileMock, THRESHOLD_BYTES));
	}

	@Test
	public void diskSpaceIsUp() throws Exception {
		given(this.fileMock.getFreeSpace()).willReturn(THRESHOLD_BYTES + 10);
		given(this.fileMock.getTotalSpace()).willReturn(THRESHOLD_BYTES * 10);
		Health health = this.healthIndicator.health();
		assertEquals(Status.UP, health.getStatus());
		assertEquals(THRESHOLD_BYTES, health.getDetails().get("threshold"));
		assertEquals(THRESHOLD_BYTES + 10, health.getDetails().get("free"));
		assertEquals(THRESHOLD_BYTES * 10, health.getDetails().get("total"));
	}

	@Test
	public void diskSpaceIsDown() throws Exception {
		given(this.fileMock.getFreeSpace()).willReturn(THRESHOLD_BYTES - 10);
		given(this.fileMock.getTotalSpace()).willReturn(THRESHOLD_BYTES * 10);
		Health health = this.healthIndicator.health();
		assertEquals(Status.DOWN, health.getStatus());
		assertEquals(THRESHOLD_BYTES, health.getDetails().get("threshold"));
		assertEquals(THRESHOLD_BYTES - 10, health.getDetails().get("free"));
		assertEquals(THRESHOLD_BYTES * 10, health.getDetails().get("total"));
	}

	private DiskSpaceHealthIndicatorProperties createProperties(File path, long threshold) {
		DiskSpaceHealthIndicatorProperties properties = new DiskSpaceHealthIndicatorProperties();
		properties.setPath(path);
		properties.setThreshold(threshold);
		return properties;
	}

}
