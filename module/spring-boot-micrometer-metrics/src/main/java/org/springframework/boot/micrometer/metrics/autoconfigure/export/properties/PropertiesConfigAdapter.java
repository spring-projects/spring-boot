/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Base class for properties to config adapters.
 *
 * @param <T> the properties type
 * @author Phillip Webb
 * @author Nikolay Rybak
 * @since 4.0.0
 */
public class PropertiesConfigAdapter<T> {

	protected final T properties;

	/**
	 * Create a new {@link PropertiesConfigAdapter} instance.
	 * @param properties the source properties
	 */
	public PropertiesConfigAdapter(T properties) {
		Assert.notNull(properties, "'properties' must not be null");
		this.properties = properties;
	}

	/**
	 * Get the value from the properties or use a fallback from the {@code defaults}.
	 * @param getter the getter for the properties
	 * @param fallback the fallback method, usually super interface method reference
	 * @param <V> the value type
	 * @return the property or fallback value
	 */
	protected final <V> @Nullable V get(Getter<T, V> getter, Supplier<@Nullable V> fallback) {
		V value = getter.get(this.properties);
		return (value != null) ? value : fallback.get();
	}

	/**
	 * Get the value from the properties or use a fallback from the {@code defaults}.
	 * @param getter the getter for the properties
	 * @param fallback the fallback method, usually super interface method reference
	 * @param <V> the value type
	 * @return the property or fallback value
	 */
	protected final <V> V getRequired(Getter<T, V> getter, Supplier<V> fallback) {
		V value = getter.get(this.properties);
		if (value != null) {
			return value;
		}
		V fallbackValue = fallback.get();
		Assert.state(fallbackValue != null, "'fallbackValue' must not be null");
		return fallbackValue;
	}

	protected interface Getter<T, V> {

		@Nullable V get(T properties);

	}

}
