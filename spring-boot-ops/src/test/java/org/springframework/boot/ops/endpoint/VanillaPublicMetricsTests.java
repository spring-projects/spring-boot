/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.ops.endpoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.ops.endpoint.VanillaPublicMetrics;
import org.springframework.boot.ops.metrics.InMemoryMetricRepository;
import org.springframework.boot.ops.metrics.Metric;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link VanillaPublicMetrics}.
 * 
 * @author Phillip Webb
 */
public class VanillaPublicMetricsTests {

	@Test
	public void testMetrics() throws Exception {
		InMemoryMetricRepository repository = new InMemoryMetricRepository();
		repository.set("a", 0.5, new Date());
		VanillaPublicMetrics publicMetrics = new VanillaPublicMetrics(repository);
		Map<String, Metric> results = new HashMap<String, Metric>();
		for (Metric metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertTrue(results.containsKey("mem"));
		assertTrue(results.containsKey("mem.free"));
		assertThat(results.get("a").getValue(), equalTo(0.5));
	}
}
