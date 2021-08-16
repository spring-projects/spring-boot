/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class DiskSpaceHealthIndicatorTests {

	private static final DataSize THRESHOLD = DataSize.ofKilobytes(1);

	private static final DataSize TOTAL_SPACE = DataSize.ofKilobytes(10);

	@Test
	void diskSpaceIsUpWithSinglePath() {
		long freeSpace = THRESHOLD.toBytes() + 10;
		File mockFile = setupMockFile("file1", freeSpace);
		HealthIndicator healthIndicator = new DiskSpaceHealthIndicator(mockFile, THRESHOLD);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertPathDetails(health, "file1", freeSpace);
	}

	@Test
	void diskSpaceIsDownWithSinglePath() {
		long freeSpace = THRESHOLD.toBytes() - 10;
		File mockFile = setupMockFile("file1", freeSpace);
		HealthIndicator healthIndicator = new DiskSpaceHealthIndicator(mockFile, THRESHOLD);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertPathDetails(health, "file1", freeSpace);
	}

	@Test
	void diskSpaceIsUpWithMultiplePaths() {
		long freeSpace = THRESHOLD.toBytes() + 10;
		File mockFile1 = setupMockFile("file1", freeSpace);
		File mockFile2 = setupMockFile("file2", freeSpace);
		Map<File, DataSize> paths = new HashMap<>();
		paths.put(mockFile1, THRESHOLD);
		paths.put(mockFile2, THRESHOLD);
		HealthIndicator healthIndicator = new DiskSpaceHealthIndicator(paths);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertPathDetails(health, "file1", freeSpace);
		assertPathDetails(health, "file2", freeSpace);
	}

	@Test
	void diskSpaceIsDownWithMultiplePathsAllOverThreshold() {
		long freeSpace = THRESHOLD.toBytes() - 10;
		File mockFile1 = setupMockFile("file1", freeSpace);
		File mockFile2 = setupMockFile("file2", freeSpace);
		Map<File, DataSize> paths = new HashMap<>();
		paths.put(mockFile1, THRESHOLD);
		paths.put(mockFile2, THRESHOLD);
		HealthIndicator healthIndicator = new DiskSpaceHealthIndicator(paths);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertPathDetails(health, "file1", freeSpace);
		assertPathDetails(health, "file2", freeSpace);
	}

	@Test
	void diskSpaceIsDownWithMultiplePathsOneOverThreshold() {
		long freeSpace = THRESHOLD.toBytes() + 10;
		File mockFile1 = setupMockFile("file1", freeSpace);

		long freeSpaceOver = THRESHOLD.toBytes() - 10;
		File mockFile2 = setupMockFile("file2", freeSpaceOver);

		Map<File, DataSize> paths = new HashMap<>();
		paths.put(mockFile1, THRESHOLD);
		paths.put(mockFile2, THRESHOLD);
		HealthIndicator healthIndicator = new DiskSpaceHealthIndicator(paths);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertPathDetails(health, "file1", freeSpace);
		assertPathDetails(health, "file2", freeSpaceOver);
	}

	@Test
	void whenPathDoesNotExistDiskSpaceIsDown() {
		File noSuchFile = new File("does/not/exist");
		Health health = new DiskSpaceHealthIndicator(noSuchFile, THRESHOLD).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).extractingByKey("paths", InstanceOfAssertFactories.MAP)
				.extractingByKey(noSuchFile.getAbsolutePath(), InstanceOfAssertFactories.MAP).containsEntry("free", 0L)
				.containsEntry("total", 0L).containsEntry("exists", false);
	}

	private File setupMockFile(String path, long usableSpace) {
		File mockFile = mock(File.class);
		given(mockFile.exists()).willReturn(true);
		given(mockFile.getAbsolutePath()).willReturn(path);
		given(mockFile.getUsableSpace()).willReturn(usableSpace);
		given(mockFile.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		return mockFile;
	}

	private void assertPathDetails(Health health, String path, long expectedFreeSpace) {
		assertThat(health.getDetails()).extractingByKey("paths", InstanceOfAssertFactories.MAP)
				.extractingByKey(path, InstanceOfAssertFactories.MAP).containsEntry("threshold", THRESHOLD.toBytes())
				.containsEntry("free", expectedFreeSpace).containsEntry("total", TOTAL_SPACE.toBytes())
				.containsEntry("exists", true);
	}

}
