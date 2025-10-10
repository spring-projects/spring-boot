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

package org.springframework.boot.actuate.endpoint;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Value object that represents the data that can be used by a {@link SanitizingFunction}.
 *
 * @author Madhura Bhave
 * @author Rohan Goyal
 * @since 2.6.0
 **/
public final class SanitizableData {

	/**
	 * Represents a sanitized value.
	 */
	public static final String SANITIZED_VALUE = "******";

	private final @Nullable PropertySource<?> propertySource;

	private final String key;

	private @Nullable String lowerCaseKey;

	private final @Nullable Object value;

	/**
	 * Create a new {@link SanitizableData} instance.
	 * @param propertySource the property source that provided the data or {@code null}.
	 * @param key the data key
	 * @param value the data value
	 */
	public SanitizableData(@Nullable PropertySource<?> propertySource, String key, @Nullable Object value) {
		Assert.notNull(key, "'key' must not be null");
		this.propertySource = propertySource;
		this.key = key;
		this.value = value;
	}

	/**
	 * Return the property source that provided the data or {@code null} If the data was
	 * not from a {@link PropertySource}.
	 * @return the property source that provided the data
	 */
	public @Nullable PropertySource<?> getPropertySource() {
		return this.propertySource;
	}

	/**
	 * Return the key of the data.
	 * @return the data key
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Return the key as a lowercase value.
	 * @return the key as a lowercase value
	 * @since 3.5.0
	 */
	public String getLowerCaseKey() {
		String result = this.lowerCaseKey;
		if (result == null) {
			result = this.key.toLowerCase(Locale.getDefault());
			this.lowerCaseKey = result;
		}
		return result;
	}

	/**
	 * Return the value of the data.
	 * @return the data value
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Return a new {@link SanitizableData} instance with sanitized value.
	 * @return a new sanitizable data instance.
	 * @since 3.1.0
	 */
	public SanitizableData withSanitizedValue() {
		return withValue(SANITIZED_VALUE);
	}

	/**
	 * Return a new {@link SanitizableData} instance with a different value.
	 * @param value the new value (often {@link #SANITIZED_VALUE}
	 * @return a new sanitizable data instance
	 */
	public SanitizableData withValue(@Nullable Object value) {
		return new SanitizableData(this.propertySource, this.key, value);
	}

}
