/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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
	File fileMock;

	HealthIndicator healthIndicator;

	@Before
	public void setUp() throws Exception {
		this.healthIndicator =
				new DiskSpaceHealthIndicator(DiskSpaceHealthIndicatorTests.class.getResource("").getPath(),
				THRESHOLD_BYTES);
		ReflectionTestUtils.setField(this.healthIndicator, "path", this.fileMock);
	}

	@Test
	public void diskSpaceIsUp() throws Exception {
		when(this.fileMock.getFreeSpace()).thenReturn(THRESHOLD_BYTES + 10);

		Health health = this.healthIndicator.health();
		assertEquals(Status.UP, health.getStatus());
	}

	@Test
	public void diskSpaceIsDown() throws Exception {
		when(this.fileMock.getFreeSpace()).thenReturn(THRESHOLD_BYTES - 10);

		Health health = this.healthIndicator.health();
		assertEquals(Status.DOWN, health.getStatus());
	}

	@Test
	public void throwsExceptionForUnknownPath() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Path does not exist: an_path_that_does_not_exist");

		new DiskSpaceHealthIndicator("an_path_that_does_not_exist", THRESHOLD_BYTES);
	}

	@Test
	public void throwsExceptionForNegativeThreshold() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("thresholdBytes must be greater than 0");

		new DiskSpaceHealthIndicator(DiskSpaceHealthIndicatorTests.class.getResource("").getPath(), -1);
	}
}