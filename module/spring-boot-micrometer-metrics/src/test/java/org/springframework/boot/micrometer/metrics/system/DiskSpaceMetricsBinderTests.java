/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.system;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DiskSpaceMetricsBinder}.
 *
 * @author Chris Bono
 */
class DiskSpaceMetricsBinderTests {

	@Test
	void diskSpaceMetricsWithSinglePath() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		File path = new File(".");
		DiskSpaceMetricsBinder metricsBinder = new DiskSpaceMetricsBinder(Collections.singletonList(path),
				Tags.empty());
		metricsBinder.bindTo(meterRegistry);

		Tags tags = Tags.of("path", path.getAbsolutePath());
		assertThat(meterRegistry.get("disk.free").tags(tags).gauge()).isNotNull();
		assertThat(meterRegistry.get("disk.total").tags(tags).gauge()).isNotNull();
	}

	@Test
	void diskSpaceMetricsWithMultiplePaths() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		File path1 = new File(".");
		File path2 = new File("..");
		DiskSpaceMetricsBinder metricsBinder = new DiskSpaceMetricsBinder(Arrays.asList(path1, path2), Tags.empty());
		metricsBinder.bindTo(meterRegistry);

		Tags tags = Tags.of("path", path1.getAbsolutePath());
		assertThat(meterRegistry.get("disk.free").tags(tags).gauge()).isNotNull();
		assertThat(meterRegistry.get("disk.total").tags(tags).gauge()).isNotNull();
		tags = Tags.of("path", path2.getAbsolutePath());
		assertThat(meterRegistry.get("disk.free").tags(tags).gauge()).isNotNull();
		assertThat(meterRegistry.get("disk.total").tags(tags).gauge()).isNotNull();
	}

	@Test
	void diskSpaceMetricsWithCustomTags() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		File path = new File(".");
		Tags customTags = Tags.of("foo", "bar");
		DiskSpaceMetricsBinder metricsBinder = new DiskSpaceMetricsBinder(Collections.singletonList(path), customTags);
		metricsBinder.bindTo(meterRegistry);

		Tags tags = Tags.of("path", path.getAbsolutePath(), "foo", "bar");
		assertThat(meterRegistry.get("disk.free").tags(tags).gauge()).isNotNull();
		assertThat(meterRegistry.get("disk.total").tags(tags).gauge()).isNotNull();
	}

}
