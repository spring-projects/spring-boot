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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link Endpoint} for exposing the metrics held by a {@link MeterRegistry}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
@Endpoint(id = "metrics")
public class MetricsEndpoint {

	private final MeterRegistry registry;

	public MetricsEndpoint(MeterRegistry registry) {
		this.registry = registry;
	}

	@ReadOperation
	public ListNamesResponse listNames() {
		Set<String> names = new LinkedHashSet<>();
		collectNames(names, this.registry);
		return new ListNamesResponse(names);
	}

	private void collectNames(Set<String> names, MeterRegistry registry) {
		if (registry instanceof CompositeMeterRegistry) {
			((CompositeMeterRegistry) registry).getRegistries()
					.forEach((member) -> collectNames(names, member));
		}
		else {
			registry.getMeters().stream().map(this::getName).forEach(names::add);
		}
	}

	private String getName(Meter meter) {
		return meter.getId().getName();
	}

	@ReadOperation
	public MetricResponse metric(@Selector String requiredMetricName,
			@Nullable List<String> tag) {
		Assert.isTrue(tag == null || tag.stream().allMatch((t) -> t.contains(":")),
				"Each tag parameter must be in the form key:value");
		List<Tag> tags = parseTags(tag);
		List<Meter> meters = new ArrayList<>();
		collectMeters(meters, this.registry, requiredMetricName, tags);
		if (meters.isEmpty()) {
			return null;
		}
		Map<Statistic, Double> samples = getSamples(meters);
		Map<String, List<String>> availableTags = getAvailableTags(meters);
		tags.forEach((t) -> availableTags.remove(t.getKey()));
		return new MetricResponse(requiredMetricName,
				asList(samples, MetricResponse.Sample::new),
				asList(availableTags, MetricResponse.AvailableTag::new));
	}

	private List<Tag> parseTags(List<String> tags) {
		return tags == null ? Collections.emptyList() : tags.stream().map((t) -> {
			String[] tagParts = t.split(":", 2);
			return Tag.of(tagParts[0], tagParts[1]);
		}).collect(Collectors.toList());
	}

	private void collectMeters(List<Meter> meters, MeterRegistry registry, String name,
			Iterable<Tag> tags) {
		if (registry instanceof CompositeMeterRegistry) {
			((CompositeMeterRegistry) registry).getRegistries()
					.forEach((member) -> collectMeters(meters, member, name, tags));
		}
		else {
			meters.addAll(registry.find(name).tags(tags).meters());
		}
	}

	private Map<Statistic, Double> getSamples(List<Meter> meters) {
		Map<Statistic, Double> samples = new LinkedHashMap<>();
		meters.forEach((meter) -> mergeMeasurements(samples, meter));
		return samples;
	}

	private void mergeMeasurements(Map<Statistic, Double> samples, Meter meter) {
		meter.measure().forEach((measurement) -> samples.merge(measurement.getStatistic(),
				measurement.getValue(), Double::sum));
	}

	private Map<String, List<String>> getAvailableTags(List<Meter> meters) {
		Map<String, List<String>> availableTags = new HashMap<>();
		meters.forEach((meter) -> mergeAvailableTags(availableTags, meter));
		return availableTags;
	}

	private void mergeAvailableTags(Map<String, List<String>> availableTags,
			Meter meter) {
		meter.getId().getTags().forEach((tag) -> {
			List<String> value = Collections.singletonList(tag.getValue());
			availableTags.merge(tag.getKey(), value, this::merge);
		});
	}

	private <T> List<T> merge(List<T> list1, List<T> list2) {
		List<T> result = new ArrayList<>(list1.size() + list2.size());
		result.addAll(list1);
		result.addAll(list2);
		return result;
	}

	private <K, V, T> List<T> asList(Map<K, V> map, BiFunction<K, V, T> mapper) {
		return map.entrySet().stream()
				.map((entry) -> mapper.apply(entry.getKey(), entry.getValue()))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Response payload for a metric name listing.
	 */
	static class ListNamesResponse {

		private final Set<String> names;

		ListNamesResponse(Set<String> names) {
			this.names = names;
		}

		public Set<String> getNames() {
			return this.names;
		}
	}

	/**
	 * Response payload for a metric name selector.
	 */
	static class MetricResponse {

		private final String name;

		private final List<Sample> measurements;

		private final List<AvailableTag> availableTags;

		MetricResponse(String name, List<Sample> measurements,
				List<AvailableTag> availableTags) {
			this.name = name;
			this.measurements = measurements;
			this.availableTags = availableTags;
		}

		public String getName() {
			return this.name;
		}

		public List<Sample> getMeasurements() {
			return this.measurements;
		}

		public List<AvailableTag> getAvailableTags() {
			return this.availableTags;
		}

		/**
		 * A set of tags for further dimensional drilldown and their potential values.
		 */
		static class AvailableTag {

			private final String tag;

			private final List<String> values;

			AvailableTag(String tag, List<String> values) {
				this.tag = tag;
				this.values = values;
			}

			public String getTag() {
				return this.tag;
			}

			public List<String> getValues() {
				return this.values;
			}
		}

		/**
		 * A measurement sample combining a {@link Statistic statistic} and a value.
		 */
		static class Sample {

			private final Statistic statistic;

			private final Double value;

			Sample(Statistic statistic, Double value) {
				this.statistic = statistic;
				this.value = value;
			}

			public Statistic getStatistic() {
				return this.statistic;
			}

			public Double getValue() {
				return this.value;
			}

			@Override
			public String toString() {
				return "MeasurementSample{" + "statistic=" + this.statistic + ", value="
						+ this.value + '}';
			}

		}

	}
}
