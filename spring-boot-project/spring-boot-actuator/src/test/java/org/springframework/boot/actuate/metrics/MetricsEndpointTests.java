/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Jon Schneider
 */
public class MetricsEndpointTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final MeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());

	private final MetricsEndpoint endpoint = new MetricsEndpoint(this.registry);

	@Test
	public void listNamesHandlesEmptyListOfMeters() {
		MetricsEndpoint.ListNamesResponse result = this.endpoint.listNames();
		assertThat(result.getNames()).isEmpty();
	}

	@Test
	public void listNamesProducesListOfUniqueMeterNames() {
		this.registry.counter("com.example.foo");
		this.registry.counter("com.example.bar");
		this.registry.counter("com.example.foo");
		MetricsEndpoint.ListNamesResponse result = this.endpoint.listNames();
		assertThat(result.getNames()).containsOnlyOnce("com.example.foo", "com.example.bar");
	}

	@Test
	public void listNamesRecursesOverCompositeRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
		SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
		composite.add(reg1);
		composite.add(reg2);
		reg1.counter("counter1").increment();
		reg2.counter("counter2").increment();
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		assertThat(endpoint.listNames().getNames()).containsOnly("counter1", "counter2");
	}

	@Test
	public void metricValuesAreTheSumOfAllTimeSeriesMatchingTags() {
		this.registry.counter("cache", "result", "hit", "host", "1").increment(2);
		this.registry.counter("cache", "result", "miss", "host", "1").increment(2);
		this.registry.counter("cache", "result", "hit", "host", "2").increment(2);
		MetricsEndpoint.MetricResponse response = this.endpoint.metric("cache", Collections.emptyList());
		assertThat(response.getName()).isEqualTo("cache");
		assertThat(availableTagKeys(response)).containsExactly("result", "host");
		assertThat(getCount(response)).hasValue(6.0);
		response = this.endpoint.metric("cache", Collections.singletonList("result:hit"));
		assertThat(availableTagKeys(response)).containsExactly("host");
		assertThat(getCount(response)).hasValue(4.0);
	}

	@Test
	public void findFirstMatchingMetersFromNestedRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry firstLevel0 = new SimpleMeterRegistry();
		CompositeMeterRegistry firstLevel1 = new CompositeMeterRegistry();
		SimpleMeterRegistry secondLevel = new SimpleMeterRegistry();
		composite.add(firstLevel0);
		composite.add(firstLevel1);
		firstLevel1.add(secondLevel);
		secondLevel.counter("cache", "result", "hit", "host", "1").increment(2);
		secondLevel.counter("cache", "result", "miss", "host", "1").increment(2);
		secondLevel.counter("cache", "result", "hit", "host", "2").increment(2);
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		MetricsEndpoint.MetricResponse response = endpoint.metric("cache", Collections.emptyList());
		assertThat(response.getName()).isEqualTo("cache");
		assertThat(availableTagKeys(response)).containsExactly("result", "host");
		assertThat(getCount(response)).hasValue(6.0);
		response = endpoint.metric("cache", Collections.singletonList("result:hit"));
		assertThat(availableTagKeys(response)).containsExactly("host");
		assertThat(getCount(response)).hasValue(4.0);
	}

	@Test
	public void matchingMeterNotFoundInNestedRegistries() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		CompositeMeterRegistry firstLevel = new CompositeMeterRegistry();
		SimpleMeterRegistry secondLevel = new SimpleMeterRegistry();
		composite.add(firstLevel);
		firstLevel.add(secondLevel);
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		MetricsEndpoint.MetricResponse response = endpoint.metric("invalid.metric.name", Collections.emptyList());
		assertThat(response).isNull();
	}

	@Test
	public void metricTagValuesAreDeduplicated() {
		this.registry.counter("cache", "host", "1", "region", "east", "result", "hit");
		this.registry.counter("cache", "host", "1", "region", "east", "result", "miss");
		MetricsEndpoint.MetricResponse response = this.endpoint.metric("cache", Collections.singletonList("host:1"));
		assertThat(response.getAvailableTags().stream().filter((t) -> t.getTag().equals("region"))
				.flatMap((t) -> t.getValues().stream())).containsExactly("east");
	}

	@Test
	public void metricWithSpaceInTagValue() {
		this.registry.counter("counter", "key", "a space").increment(2);
		MetricsEndpoint.MetricResponse response = this.endpoint.metric("counter",
				Collections.singletonList("key:a space"));
		assertThat(response.getName()).isEqualTo("counter");
		assertThat(availableTagKeys(response)).isEmpty();
		assertThat(getCount(response)).hasValue(2.0);
	}

	@Test
	public void metricWithInvalidTag() {
		this.thrown.expect(InvalidEndpointRequestException.class);
		this.endpoint.metric("counter", Collections.singletonList("key"));
	}

	@Test
	public void metricPresentInOneRegistryOfACompositeAndNotAnother() {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
		SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
		composite.add(reg1);
		composite.add(reg2);
		reg1.counter("counter1").increment();
		reg2.counter("counter2").increment();
		MetricsEndpoint endpoint = new MetricsEndpoint(composite);
		assertThat(endpoint.metric("counter1", Collections.emptyList())).isNotNull();
		assertThat(endpoint.metric("counter2", Collections.emptyList())).isNotNull();
	}

	@Test
	public void nonExistentMetric() {
		MetricsEndpoint.MetricResponse response = this.endpoint.metric("does.not.exist", Collections.emptyList());
		assertThat(response).isNull();
	}

	@Test
	public void maxAggregation() {
		SimpleMeterRegistry reg = new SimpleMeterRegistry();
		reg.timer("timer", "k", "v1").record(1, TimeUnit.SECONDS);
		reg.timer("timer", "k", "v2").record(2, TimeUnit.SECONDS);
		assertMetricHasStatisticEqualTo(reg, "timer", Statistic.MAX, 2.0);
	}

	@Test
	public void countAggregation() {
		SimpleMeterRegistry reg = new SimpleMeterRegistry();
		reg.counter("counter", "k", "v1").increment();
		reg.counter("counter", "k", "v2").increment();
		assertMetricHasStatisticEqualTo(reg, "counter", Statistic.COUNT, 2.0);
	}

	private void assertMetricHasStatisticEqualTo(MeterRegistry registry, String metricName, Statistic stat,
			Double value) {
		MetricsEndpoint endpoint = new MetricsEndpoint(registry);
		assertThat(endpoint.metric(metricName, Collections.emptyList()).getMeasurements().stream()
				.filter((sample) -> sample.getStatistic().equals(stat)).findAny())
						.hasValueSatisfying((sample) -> assertThat(sample.getValue()).isEqualTo(value));
	}

	private Optional<Double> getCount(MetricsEndpoint.MetricResponse response) {
		return response.getMeasurements().stream().filter((sample) -> sample.getStatistic().equals(Statistic.COUNT))
				.findAny().map(MetricsEndpoint.Sample::getValue);
	}

	private Stream<String> availableTagKeys(MetricsEndpoint.MetricResponse response) {
		return response.getAvailableTags().stream().map(MetricsEndpoint.AvailableTag::getTag);
	}

}
