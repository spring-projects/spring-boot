/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/**
 * Tests for {@link VanillaPublicMetrics}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
public class VanillaPublicMetricsTests {

	@Test
	public void testMetrics() throws Exception {
		InMemoryMetricRepository repository = new InMemoryMetricRepository();
		repository.set(new Metric<Double>("a", 0.5, new Date()));
		VanillaPublicMetrics publicMetrics = new VanillaPublicMetrics(repository);
		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertThat(results.get("a").getValue().doubleValue(), equalTo(0.5));
	}

	@Test
	public void testAdditionalMetrics() throws Exception {
		InMemoryMetricRepository repository = new InMemoryMetricRepository();
		Collection<PublicMetrics> allMetrics = new ArrayList<PublicMetrics>();
		allMetrics.add(new ImmutablePublicMetrics(new Metric<Number>("first", 2L)));
		allMetrics.add(new ImmutablePublicMetrics(new Metric<Number>("second", 4L)));

		VanillaPublicMetrics publicMetrics = new VanillaPublicMetrics(repository, allMetrics);
		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertTrue(results.containsKey("first"));
		assertTrue(results.containsKey("second"));
		assertEquals(2, results.size());
	}


	private static class ImmutablePublicMetrics implements PublicMetrics {
		private final Collection<Metric<?>> metrics;

		private ImmutablePublicMetrics(Metric<?> metrics) {
			this.metrics = new LinkedHashSet<Metric<?>>();
			this.metrics.addAll(Arrays.asList(metrics));
		}

		@Override
		public Collection<Metric<?>> metrics() {
			return metrics;
		}
	}
}
