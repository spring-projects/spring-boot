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

package org.springframework.boot.context.config;

import org.springframework.boot.origin.Origin;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigDataLocation} cannot be
 * found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigDataLocationNotFoundException extends ConfigDataNotFoundException {

	private final ConfigDataLocation location;

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
		this(location, null);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 * @param cause the exception cause
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location, Throwable cause) {
		this(location, getMessage(location), cause);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 * @param message the exception message
	 * @param cause the exception cause
	 * @since 2.4.7
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location, String message, Throwable cause) {
		super(message, cause);
		Assert.notNull(location, "Location must not be null");
		this.location = location;
	}

	/**
	 * Return the location that could not be found.
	 * @return the location
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

	@Override
	public Origin getOrigin() {
		return Origin.from(this.location);
	}

	@Override
	public String getReferenceDescription() {
		return getReferenceDescription(this.location);
	}

	private static String getMessage(ConfigDataLocation location) {
		return String.format("Config data %s cannot be found", getReferenceDescription(location));
	}

	private static String getReferenceDescription(ConfigDataLocation location) {
		return String.format("location '%s'", location);
	}

}
