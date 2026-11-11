/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.application;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class DiskSpaceHealthIndicatorTests {

	private static final DataSize THRESHOLD = DataSize.ofKilobytes(1);

	private static final DataSize TOTAL_SPACE = DataSize.ofKilobytes(10);

	@Mock
	private File fileMock;

	private HealthIndicator healthIndicator;

	@BeforeEach
	void setUp() {
		this.healthIndicator = new DiskSpaceHealthIndicator(this.fileMock, THRESHOLD);
	}

	@Test
	void diskSpaceIsUp() {
		given(this.fileMock.exists()).willReturn(true);
		long freeSpace = THRESHOLD.toBytes() + 10;
		given(this.fileMock.getUsableSpace()).willReturn(freeSpace);
		given(this.fileMock.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		given(this.fileMock.getAbsolutePath()).willReturn("/absolute-path");
		Health health = this.healthIndicator.health();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("threshold", THRESHOLD.toBytes());
		assertThat(health.getDetails()).containsEntry("free", freeSpace);
		assertThat(health.getDetails()).containsEntry("total", TOTAL_SPACE.toBytes());
		assertThat(health.getDetails()).containsEntry("path", "/absolute-path");
		assertThat(health.getDetails()).containsEntry("exists", true);
	}

	@Test
	void diskSpaceIsDown() {
		given(this.fileMock.exists()).willReturn(true);
		long freeSpace = THRESHOLD.toBytes() - 10;
		given(this.fileMock.getUsableSpace()).willReturn(freeSpace);
		given(this.fileMock.getTotalSpace()).willReturn(TOTAL_SPACE.toBytes());
		given(this.fileMock.getAbsolutePath()).willReturn("/absolute-path");
		Health health = this.healthIndicator.health();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("threshold", THRESHOLD.toBytes());
		assertThat(health.getDetails()).containsEntry("free", freeSpace);
		assertThat(health.getDetails()).containsEntry("total", TOTAL_SPACE.toBytes());
		assertThat(health.getDetails()).containsEntry("path", "/absolute-path");
		assertThat(health.getDetails()).containsEntry("exists", true);
	}

	@Test
	void whenPathDoesNotExistDiskSpaceIsDown() {
		Health health = new DiskSpaceHealthIndicator(new File("does/not/exist"), THRESHOLD).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("free", 0L);
		assertThat(health.getDetails()).containsEntry("total", 0L);
		assertThat(health.getDetails()).containsEntry("exists", false);
	}

}
