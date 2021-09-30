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

package org.springframework.boot.actuate.metrics.system;

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
