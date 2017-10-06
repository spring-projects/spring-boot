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

package org.springframework.boot.actuate.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Jon Schneider
 */
public class MetricsEndpointTests {

	private final MeterRegistry registry = new SimpleMeterRegistry();

	private final MetricsEndpoint endpoint = new MetricsEndpoint(this.registry);

	@Test
	public void listNamesHandlesEmptyListOfMeters() {
		Map<String, List<String>> result = this.endpoint.listNames();
		assertThat(result).containsOnlyKeys("names");
		assertThat(result.get("names")).isEmpty();
	}

	@Test
	public void listNamesProducesListOfUniqueMeterNames() {
		this.registry.counter("com.example.foo");
		this.registry.counter("com.example.bar");
		this.registry.counter("com.example.foo");
		Map<String, List<String>> result = this.endpoint.listNames();
		assertThat(result).containsOnlyKeys("names");
		assertThat(result.get("names")).containsOnlyOnce("com.example.foo",
				"com.example.bar");
	}

	@Test
	public void metricValuesAreTheSumOfAllTimeSeriesMatchingTags() {
		this.registry.counter("cache", "result", "hit", "host", "1").increment(2);
		this.registry.counter("cache", "result", "miss", "host", "1").increment(2);
		this.registry.counter("cache", "result", "hit", "host", "2").increment(2);
		MetricsEndpoint.Response response = this.endpoint.metric("cache",
				Collections.emptyList());
		assertThat(response.getName()).isEqualTo("cache");
		assertThat(availableTagKeys(response)).containsExactly("result", "host");
		assertThat(getCount(response)).hasValue(6.0);
		response = this.endpoint.metric("cache", Collections.singletonList("result:hit"));
		assertThat(availableTagKeys(response)).containsExactly("host");
		assertThat(getCount(response)).hasValue(4.0);
	}

	@Test
	public void metricWithSpaceInTagValue() {
		this.registry.counter("counter", "key", "a space").increment(2);
		MetricsEndpoint.Response response = this.endpoint.metric("counter",
				Collections.singletonList("key:a space"));
		assertThat(response.getName()).isEqualTo("counter");
		assertThat(availableTagKeys(response)).isEmpty();
		assertThat(getCount(response)).hasValue(2.0);
	}

	@Test
	public void nonExistentMetric() {
		MetricsEndpoint.Response response = this.endpoint.metric("does.not.exist",
				Collections.emptyList());
		assertThat(response).isNull();
	}

	private Optional<Double> getCount(MetricsEndpoint.Response response) {
		return response.getMeasurements().stream()
				.filter((ms) -> ms.getStatistic().equals(Statistic.Count)).findAny()
				.map(MetricsEndpoint.Response.Sample::getValue);
	}

	private Stream<String> availableTagKeys(MetricsEndpoint.Response response) {
		return response.getAvailableTags().stream()
				.map(MetricsEndpoint.Response.AvailableTag::getTag);
	}

}
