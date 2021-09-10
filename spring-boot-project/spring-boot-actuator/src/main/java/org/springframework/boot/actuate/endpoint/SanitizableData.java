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

package org.springframework.boot.actuate.endpoint;

import org.springframework.core.env.PropertySource;

/**
 * Value object that represents the data that can be used by a {@link SanitizingFunction}.
 *
 * @author Madhura Bhave
 * @since 2.6.0
 **/
public final class SanitizableData {

	/**
	 * Represents a sanitized value.
	 */
	public static final String SANITIZED_VALUE = "******";

	private final PropertySource<?> propertySource;

	private final String key;

	private final Object value;

	/**
	 * Create a new {@link SanitizableData} instance.
	 * @param propertySource the property source that provided the data or {@code null}.
	 * @param key the data key
	 * @param value the data value
	 */
	public SanitizableData(PropertySource<?> propertySource, String key, Object value) {
		this.propertySource = propertySource;
		this.key = key;
		this.value = value;
	}

	/**
	 * Return the property source that provided the data or {@code null} If the data was
	 * not from a {@link PropertySource}.
	 * @return the property source that provided the data
	 */
	public PropertySource<?> getPropertySource() {
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
	 * Return the value of the data.
	 * @return the data value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return a new {@link SanitizableData} instance with a different value.
	 * @param value the new value (often {@link #SANITIZED_VALUE}
	 * @return a new sanitizable data instance
	 */
	public SanitizableData withValue(Object value) {
		return new SanitizableData(this.propertySource, this.key, value);
	}

}
