/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.springframework.util.Assert;

/**
 * Exception thrown when a configuration property value is invalid.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class InvalidConfigurationPropertyValueException extends RuntimeException {

	private final String name;

	private final Object value;

	private final String reason;

	/**
	 * Creates a new instance for the specified property {@code name} and {@code value},
	 * including a {@code reason} why the value is invalid.
	 * @param name the name of the property in canonical format
	 * @param value the value of the property, can be {@code null}
	 * @param reason a human-readable text that describes why the reason is invalid.
	 * Starts with an upper-case and ends with a dots. Several sentences and carriage
	 * returns are allowed.
	 */
	public InvalidConfigurationPropertyValueException(String name, Object value,
			String reason) {
		super("Property " + name + " with value '" + value + "' is invalid: " + reason);
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
		this.reason = reason;
	}

	/**
	 * Return the name of the property.
	 * @return the property name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the invalid value, can be {@code null}.
	 * @return the invalid value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the reason why the value is invalid.
	 * @return the reason
	 */
	public String getReason() {
		return this.reason;
	}

}
