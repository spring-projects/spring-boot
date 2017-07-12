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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MetricsEndpointTests {

	private Metric<Number> metric1 = new Metric<>("a", 1);

	private Metric<Number> metric2 = new Metric<>("b", 2);

	private Metric<Number> metric3 = new Metric<>("c", 3);

	@Test
	public void basicMetrics() throws Exception {
		MetricsEndpoint endpoint = new MetricsEndpoint(
				Collections.singletonList(() -> Arrays.asList(new Metric<Number>("a", 5),
						new Metric<Number>("b", 4))));
		Map<String, Object> metrics = endpoint.metrics(null);
		assertThat(metrics.get("a")).isEqualTo(5);
		assertThat(metrics.get("b")).isEqualTo(4);
	}

	@Test
	public void metricsOrderingIsReflectedInOutput() {
		List<PublicMetrics> publicMetrics = Arrays.asList(
				new TestPublicMetrics(2, this.metric2, this.metric2, this.metric3),
				new TestPublicMetrics(1, this.metric1));
		Map<String, Object> metrics = new MetricsEndpoint(publicMetrics).metrics(null);
		assertThat(metrics.keySet()).containsExactly("a", "b", "c");
		assertThat(metrics).hasSize(3);
	}

	@Test
	public void singleSelectedMetric() {
		List<PublicMetrics> publicMetrics = Arrays.asList(
				new TestPublicMetrics(2, this.metric2, this.metric2, this.metric3),
				new TestPublicMetrics(1, this.metric1));
		Map<String, Object> selected = new MetricsEndpoint(publicMetrics)
				.metricNamed("a");
		assertThat(selected).hasSize(1);
		assertThat(selected).containsEntry("a", 1);
	}

	@Test
	public void multipleSelectedMetrics() {
		List<PublicMetrics> publicMetrics = Arrays.asList(
				new TestPublicMetrics(2, this.metric2, this.metric2, this.metric3),
				new TestPublicMetrics(1, this.metric1));
		Map<String, Object> selected = new MetricsEndpoint(publicMetrics).metrics("[ab]");
		assertThat(selected).hasSize(2);
		assertThat(selected).containsEntry("a", 1);
		assertThat(selected).containsEntry("b", 2);
	}

	@Test
	public void noMetricMatchingName() {
		List<PublicMetrics> publicMetrics = Arrays.asList(
				new TestPublicMetrics(2, this.metric2, this.metric2, this.metric3),
				new TestPublicMetrics(1, this.metric1));
		assertThat(new MetricsEndpoint(publicMetrics).metricNamed("z")).isNull();
	}

	@Test
	public void noMetricMatchingPattern() {
		List<PublicMetrics> publicMetrics = Arrays.asList(
				new TestPublicMetrics(2, this.metric2, this.metric2, this.metric3),
				new TestPublicMetrics(1, this.metric1));
		assertThat(new MetricsEndpoint(publicMetrics).metrics("[z]")).isEmpty();
	}

	private static class TestPublicMetrics implements PublicMetrics, Ordered {

		private final int order;

		private final List<Metric<?>> metrics;

		TestPublicMetrics(int order, Metric<?>... metrics) {
			this.order = order;
			this.metrics = Arrays.asList(metrics);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public Collection<Metric<?>> metrics() {
			return this.metrics;
		}

	}

}
