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

	PropertiesObservationFilterPredicate(ObservationProperties properties) {
		this.properties = properties;
		this.commonKeyValuesFilter = createCommonKeyValuesFilter(properties);
	}

	@Override
	public Context map(Context context) {
		return this.commonKeyValuesFilter.map(context);
	}

	@Override
	public boolean test(String name, Context context) {
		return lookupWithFallbackToAll(this.properties.getEnable(), name, true);
	}

	private static <T> T lookupWithFallbackToAll(Map<String, T> values, String name, T defaultValue) {
		if (values.isEmpty()) {
			return defaultValue;
		}
		return doLookup(values, name, () -> values.getOrDefault("all", defaultValue));
	}

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

	private static ObservationFilter createCommonKeyValuesFilter(ObservationProperties properties) {
		if (properties.getKeyValues().isEmpty()) {
			return (context) -> context;
		}
		KeyValues keyValues = KeyValues.of(properties.getKeyValues().entrySet(), Entry::getKey, Entry::getValue);
		return (context) -> context.addLowCardinalityKeyValues(keyValues);
	}

}
