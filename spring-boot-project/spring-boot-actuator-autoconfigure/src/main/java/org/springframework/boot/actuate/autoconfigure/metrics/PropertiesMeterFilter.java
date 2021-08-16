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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Distribution;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link MeterFilter} to apply settings from {@link MetricsProperties}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @author Alexander Abramov
 * @since 2.0.0
 */
public class PropertiesMeterFilter implements MeterFilter {

	private final MetricsProperties properties;

	private final MeterFilter mapFilter;

	public PropertiesMeterFilter(MetricsProperties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
		this.mapFilter = createMapFilter(properties.getTags());
	}

	private static MeterFilter createMapFilter(Map<String, String> tags) {
		if (tags.isEmpty()) {
			return new MeterFilter() {
			};
		}
		Tags commonTags = Tags.of(tags.entrySet().stream().map((entry) -> Tag.of(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList()));
		return MeterFilter.commonTags(commonTags);
	}

	@Override
	public MeterFilterReply accept(Meter.Id id) {
		boolean enabled = lookupWithFallbackToAll(this.properties.getEnable(), id, true);
		return enabled ? MeterFilterReply.NEUTRAL : MeterFilterReply.DENY;
	}

	@Override
	public Id map(Id id) {
		return this.mapFilter.map(id);
	}

	@Override
	public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
		Distribution distribution = this.properties.getDistribution();
		return DistributionStatisticConfig.builder()
				.percentilesHistogram(lookupWithFallbackToAll(distribution.getPercentilesHistogram(), id, null))
				.percentiles(lookupWithFallbackToAll(distribution.getPercentiles(), id, null))
				.serviceLevelObjectives(
						convertServiceLevelObjectives(id.getType(), lookup(distribution.getSlo(), id, null)))
				.minimumExpectedValue(
						convertMeterValue(id.getType(), lookup(distribution.getMinimumExpectedValue(), id, null)))
				.maximumExpectedValue(
						convertMeterValue(id.getType(), lookup(distribution.getMaximumExpectedValue(), id, null)))
				.expiry(lookupWithFallbackToAll(distribution.getExpiry(), id, null))
				.bufferLength(lookupWithFallbackToAll(distribution.getBufferLength(), id, null)).build().merge(config);
	}

	private double[] convertServiceLevelObjectives(Meter.Type meterType, ServiceLevelObjectiveBoundary[] slo) {
		if (slo == null) {
			return null;
		}
		double[] converted = Arrays.stream(slo).map((candidate) -> candidate.getValue(meterType))
				.filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
		return (converted.length != 0) ? converted : null;
	}

	private Double convertMeterValue(Meter.Type meterType, String value) {
		return (value != null) ? MeterValue.valueOf(value).getValue(meterType) : null;
	}

	private <T> T lookup(Map<String, T> values, Id id, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, id, () -> defaultValue);
	}

	private <T> T lookupWithFallbackToAll(Map<String, T> values, Id id, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, id, () -> values.getOrDefault("all", defaultValue));
	}

	private <T> T doLookup(Map<String, T> values, Id id, Supplier<T> defaultValue) {
		String name = id.getName();
		while (StringUtils.hasLength(name)) {
			T result = values.get(name);
			if (result != null) {
				return result;
			}
			int lastDot = name.lastIndexOf('.');
			name = (lastDot != -1) ? name.substring(0, lastDot) : "";
		}

		return defaultValue.get();
	}

}
