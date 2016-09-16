/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemPublicMetrics}
 *
 * @author Stephane Nicoll
 */
public class SystemPublicMetricsTests {

	@Test
	public void testSystemMetrics() throws Exception {
		SystemPublicMetrics publicMetrics = new SystemPublicMetrics();
		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertThat(results).containsKey("mem");
		assertThat(results).containsKey("mem.free");
		assertThat(results).containsKey("processors");
		assertThat(results).containsKey("uptime");
		assertThat(results).containsKey("systemload.average");
		assertThat(results).containsKey("heap.committed");
		assertThat(results).containsKey("heap.init");
		assertThat(results).containsKey("heap.used");
		assertThat(results).containsKey("heap");
		assertThat(results).containsKey("nonheap.committed");
		assertThat(results).containsKey("nonheap.init");
		assertThat(results).containsKey("nonheap.used");
		assertThat(results).containsKey("nonheap");
		assertThat(results).containsKey("threads.peak");
		assertThat(results).containsKey("threads.daemon");
		assertThat(results).containsKey("threads.totalStarted");
		assertThat(results).containsKey("threads");
		assertThat(results).containsKey("classes.loaded");
		assertThat(results).containsKey("classes.unloaded");
		assertThat(results).containsKey("classes");
	}

}
