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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Measurement;
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
		return new ListNamesResponse(recurseListNames(this.registry));
	}

	private Set<String> recurseListNames(MeterRegistry registry) {
		Set<String> names = new HashSet<>();
		if (registry instanceof CompositeMeterRegistry) {
			for (MeterRegistry compositeMember : ((CompositeMeterRegistry) registry)
					.getRegistries()) {
				names.addAll(recurseListNames(compositeMember));
			}
		}
		else {
			registry.getMeters().stream().map(this::getMeterIdName).forEach(names::add);
		}
		return names;
	}

	private String getMeterIdName(Meter meter) {
		return meter.getId().getName();
	}

	@ReadOperation
	public MetricResponse metric(@Selector String requiredMetricName,
			@Nullable List<String> tag) {
		Assert.isTrue(tag == null || tag.stream().allMatch((t) -> t.contains(":")),
				"Each tag parameter must be in the form key:value");
		List<Tag> tags = parseTags(tag);
		Collection<Meter> meters = recurseFindMeter(this.registry, requiredMetricName, tags);
		if (meters.isEmpty()) {
			return null;
		}

		Map<Statistic, Double> samples = new HashMap<>();
		Map<String, List<String>> availableTags = new HashMap<>();

		for (Meter meter : meters) {
			for (Measurement ms : meter.measure()) {
				samples.merge(ms.getStatistic(), ms.getValue(), Double::sum);
			}
			for (Tag availableTag : meter.getId().getTags()) {
				availableTags.merge(availableTag.getKey(),
						Collections.singletonList(availableTag.getValue()),
						(t1, t2) -> Stream.concat(t1.stream(), t2.stream())
								.collect(Collectors.toList()));
			}
		}

		tags.forEach((t) -> availableTags.remove(t.getKey()));

		return new MetricResponse(requiredMetricName,
				samples.entrySet().stream()
						.map((sample) -> new MetricResponse.Sample(sample.getKey(),
								sample.getValue()))
						.collect(Collectors.toList()),
				availableTags.entrySet().stream()
						.map((tagValues) -> new MetricResponse.AvailableTag(
								tagValues.getKey(), tagValues.getValue()))
						.collect(Collectors.toList()));
	}

	private Collection<Meter> recurseFindMeter(MeterRegistry registry, String name,
			Iterable<Tag> tags) {
		Collection<Meter> meters = new ArrayList<>();
		if (registry instanceof CompositeMeterRegistry) {
			for (MeterRegistry compositeMember : ((CompositeMeterRegistry) registry)
					.getRegistries()) {
				meters.addAll(recurseFindMeter(compositeMember, name, tags));
			}
		}
		else {
			meters.addAll(registry.find(name).tags(tags).meters());
		}
		return meters;
	}

	private List<Tag> parseTags(List<String> tags) {
		return tags == null ? Collections.emptyList() : tags.stream().map((t) -> {
			String[] tagParts = t.split(":", 2);
			return Tag.of(tagParts[0], tagParts[1]);
		}).collect(Collectors.toList());
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
