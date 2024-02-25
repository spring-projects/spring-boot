/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

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

	/**
	 * Constructs a new PropertiesMeterFilter with the given MetricsProperties.
	 * @param properties the MetricsProperties to be used for filtering
	 * @throws IllegalArgumentException if properties is null
	 */
	public PropertiesMeterFilter(MetricsProperties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
		this.mapFilter = createMapFilter(properties.getTags());
	}

	/**
	 * Creates a MeterFilter based on the provided tags.
	 * @param tags the map of tags to be used for filtering
	 * @return a MeterFilter object based on the provided tags
	 */
	private static MeterFilter createMapFilter(Map<String, String> tags) {
		if (tags.isEmpty()) {
			return new MeterFilter() {
			};
		}
		Tags commonTags = Tags.of(tags.entrySet().stream().map(PropertiesMeterFilter::asTag).toList());
		return MeterFilter.commonTags(commonTags);
	}

	/**
	 * Converts an entry of type {@code Entry<String, String>} to a {@code Tag} object.
	 * @param entry the entry to be converted
	 * @return the converted {@code Tag} object
	 */
	private static Tag asTag(Entry<String, String> entry) {
		return Tag.of(entry.getKey(), entry.getValue());
	}

	/**
	 * Determines whether a meter should be accepted or denied based on the provided meter
	 * ID and the enable property.
	 * @param id The meter ID to be checked.
	 * @return The meter filter reply, either NEUTRAL if the meter is enabled or DENY if
	 * the meter is disabled.
	 */
	@Override
	public MeterFilterReply accept(Meter.Id id) {
		boolean enabled = lookupWithFallbackToAll(this.properties.getEnable(), id, true);
		return enabled ? MeterFilterReply.NEUTRAL : MeterFilterReply.DENY;
	}

	/**
	 * Maps the given Id using the mapFilter.
	 * @param id the Id to be mapped
	 * @return the mapped Id
	 */
	@Override
	public Id map(Id id) {
		return this.mapFilter.map(id);
	}

	/**
	 * Configures the distribution statistic for a meter.
	 * @param id the meter ID
	 * @param config the existing distribution statistic configuration
	 * @return the updated distribution statistic configuration
	 */
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
			.bufferLength(lookupWithFallbackToAll(distribution.getBufferLength(), id, null))
			.build()
			.merge(config);
	}

	/**
	 * Converts an array of ServiceLevelObjectiveBoundary objects to an array of doubles
	 * based on the provided meter type.
	 * @param meterType the type of meter
	 * @param slo the array of ServiceLevelObjectiveBoundary objects
	 * @return an array of doubles representing the converted
	 * ServiceLevelObjectiveBoundary values, or null if the input array is null or empty
	 */
	private double[] convertServiceLevelObjectives(Meter.Type meterType, ServiceLevelObjectiveBoundary[] slo) {
		if (slo == null) {
			return null;
		}
		double[] converted = Arrays.stream(slo)
			.map((candidate) -> candidate.getValue(meterType))
			.filter(Objects::nonNull)
			.mapToDouble(Double::doubleValue)
			.toArray();
		return (converted.length != 0) ? converted : null;
	}

	/**
	 * Converts a meter value from a string representation to a double value based on the
	 * specified meter type.
	 * @param meterType the type of the meter
	 * @param value the string representation of the meter value
	 * @return the converted meter value as a double, or null if the input value is null
	 */
	private Double convertMeterValue(Meter.Type meterType, String value) {
		return (value != null) ? MeterValue.valueOf(value).getValue(meterType) : null;
	}

	/**
	 * Looks up a value in the given map based on the provided id. If the map is empty,
	 * the default value is returned.
	 * @param values the map containing the values to be looked up
	 * @param id the id used to search for the value in the map
	 * @param defaultValue the default value to be returned if the map is empty
	 * @return the value associated with the id in the map, or the default value if the
	 * map is empty
	 */
	private <T> T lookup(Map<String, T> values, Id id, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, id, () -> defaultValue);
	}

	/**
	 * Looks up a value in the given map using the specified ID, with a fallback to the
	 * "all" key if the map is not empty.
	 * @param <T> the type of the values in the map
	 * @param values the map containing the values to be looked up
	 * @param id the ID used to lookup the value in the map
	 * @param defaultValue the default value to be returned if the map is empty or the
	 * lookup fails
	 * @return the value associated with the specified ID, or the default value if the map
	 * is empty or the lookup fails
	 */
	private <T> T lookupWithFallbackToAll(Map<String, T> values, Id id, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, id, () -> values.getOrDefault("all", defaultValue));
	}

	/**
	 * Performs a lookup in the given map of values using the provided Id.
	 * @param values the map of values to perform the lookup on
	 * @param id the Id object used for the lookup
	 * @param defaultValue a supplier function that provides a default value if no match
	 * is found
	 * @return the value found in the map, or the default value if no match is found
	 */
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
