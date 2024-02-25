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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationPredicate;

import org.springframework.util.StringUtils;

/**
 * {@link ObservationFilter} to apply settings from {@link ObservationProperties}.
 *
 * @author Moritz Halbritter
 */
class PropertiesObservationFilterPredicate implements ObservationFilter, ObservationPredicate {

	private final ObservationFilter commonKeyValuesFilter;

	private final ObservationProperties properties;

	/**
	 * Constructs a new PropertiesObservationFilterPredicate with the specified
	 * ObservationProperties.
	 * @param properties the ObservationProperties to be used for filtering observations
	 */
	PropertiesObservationFilterPredicate(ObservationProperties properties) {
		this.properties = properties;
		this.commonKeyValuesFilter = createCommonKeyValuesFilter(properties);
	}

	/**
	 * Maps the given context using the commonKeyValuesFilter.
	 * @param context the context to be mapped
	 * @return the mapped context
	 */
	@Override
	public Context map(Context context) {
		return this.commonKeyValuesFilter.map(context);
	}

	/**
	 * Tests the given name with the provided context.
	 * @param name the name to be tested
	 * @param context the context in which the name is tested
	 * @return true if the name passes the test, false otherwise
	 */
	@Override
	public boolean test(String name, Context context) {
		return lookupWithFallbackToAll(this.properties.getEnable(), name, true);
	}

	/**
	 * Looks up a value in a map with a fallback to a default value if the map is empty.
	 * @param values the map containing the values to be looked up
	 * @param name the name of the value to be looked up
	 * @param defaultValue the default value to be returned if the map is empty or the
	 * value is not found
	 * @return the value associated with the given name, or the default value if the map
	 * is empty or the value is not found
	 */
	private static <T> T lookupWithFallbackToAll(Map<String, T> values, String name, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, name, () -> values.getOrDefault("all", defaultValue));
	}

	/**
	 * Performs a lookup in the given map of values using the specified name. If a value
	 * is found, it is returned. If not, the lookup is performed recursively by removing
	 * the last dot-separated segment of the name until a value is found or the name
	 * becomes empty. If no value is found, the default value provided by the supplier is
	 * returned.
	 * @param <T> the type of the values in the map
	 * @param values the map of values to perform the lookup on
	 * @param name the name to lookup in the map
	 * @param defaultValue the supplier that provides the default value if no value is
	 * found
	 * @return the value found in the map, or the default value if no value is found
	 */
	private static <T> T doLookup(Map<String, T> values, String name, Supplier<T> defaultValue) {
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

	/**
	 * Creates a filter based on common key values.
	 * @param properties the observation properties
	 * @return the observation filter
	 */
	private static ObservationFilter createCommonKeyValuesFilter(ObservationProperties properties) {
		if (properties.getKeyValues().isEmpty()) {
			return (context) -> context;
		}
		KeyValues keyValues = KeyValues.of(properties.getKeyValues().entrySet(), Entry::getKey, Entry::getValue);
		return (context) -> context.addLowCardinalityKeyValues(keyValues);
	}

}
