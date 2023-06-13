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

import java.util.Map.Entry;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationFilter;

/**
 * {@link ObservationFilter} to apply settings from {@link ObservationProperties}.
 *
 * @author Moritz Halbritter
 */
class PropertiesObservationFilter implements ObservationFilter {

	private final ObservationFilter delegate;

	PropertiesObservationFilter(ObservationProperties properties) {
		this.delegate = createDelegate(properties);
	}

	@Override
	public Context map(Context context) {
		return this.delegate.map(context);
	}

	private static ObservationFilter createDelegate(ObservationProperties properties) {
		if (properties.getKeyValues().isEmpty()) {
			return (context) -> context;
		}
		KeyValues keyValues = KeyValues.of(properties.getKeyValues().entrySet(), Entry::getKey, Entry::getValue);
		return (context) -> context.addLowCardinalityKeyValues(keyValues);
	}

}
