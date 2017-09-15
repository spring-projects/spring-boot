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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.NamingConvention;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

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
	public Map<String, List<String>> listNames() {
		return Collections.singletonMap("names", this.registry.getMeters().stream()
				.map(this::getMeterIdName).collect(Collectors.toList()));
	}

	private String getMeterIdName(Meter meter) {
		return meter.getId().getName();
	}

	@ReadOperation
	public Map<String, Collection<MeasurementSample>> metric(
			@Selector String requiredMetricName) {
		return this.registry.find(requiredMetricName).meters().stream()
				.collect(Collectors.toMap(this::getHierarchicalName, this::getSamples));
	}

	private List<MeasurementSample> getSamples(Meter meter) {
		return stream(meter.measure()).map(this::getSample).collect(Collectors.toList());
	}

	private MeasurementSample getSample(Measurement measurement) {
		return new MeasurementSample(measurement.getStatistic(), measurement.getValue());
	}

	private String getHierarchicalName(Meter meter) {
		return HierarchicalNameMapper.DEFAULT.toHierarchicalName(meter.getId(),
				NamingConvention.camelCase);
	}

	private <T> Stream<T> stream(Iterable<T> measure) {
		return StreamSupport.stream(measure.spliterator(), false);
	}

	/**
	 * A measurement sample combining a {@link Statistic statistic} and a value.
	 */
	static class MeasurementSample {

		private final Statistic statistic;

		private final Double value;

		MeasurementSample(Statistic statistic, Double value) {
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
