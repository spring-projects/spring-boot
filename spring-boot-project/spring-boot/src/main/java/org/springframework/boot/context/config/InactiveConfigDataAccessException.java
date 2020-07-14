/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.env.PropertySource;

/**
 * Exception thrown when an attempt is made to resolve a property against an inactive
 * {@link ConfigData} property source. Used to ensure that a user doesn't accidentally
 * attempt to specify a properties that can never be resolved.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class InactiveConfigDataAccessException extends ConfigDataException {

	private final PropertySource<?> propertySource;

	private final ConfigDataLocation location;

	private final String propertyName;

	private final Origin origin;

	/**
	 * Create a new {@link InactiveConfigDataAccessException} instance.
	 * @param propertySource the inactive property source
	 * @param location the {@link ConfigDataLocation} of the property source or
	 * {@code null} if the source was not loaded from {@link ConfigData}.
	 * @param propertyName the name of the property
	 * @param origin the origin or the property or {@code null}
	 */
	InactiveConfigDataAccessException(PropertySource<?> propertySource, ConfigDataLocation location,
			String propertyName, Origin origin) {
		super(getMessage(propertySource, location, propertyName, origin), null);
		this.propertySource = propertySource;
		this.location = location;
		this.propertyName = propertyName;
		this.origin = origin;
	}

	private static String getMessage(PropertySource<?> propertySource, ConfigDataLocation location, String propertyName,
			Origin origin) {
		StringBuilder message = new StringBuilder("Inactive property source '");
		message.append(propertySource.getName());
		if (location != null) {
			message.append("' imported from location '");
			message.append(location);
		}
		message.append("' cannot contain property '");
		message.append(propertyName);
		message.append("'");
		if (origin != null) {
			message.append(" [origin: ");
			message.append(origin);
			message.append("]");
		}
		return message.toString();
	}

	/**
	 * Return the inactive property source that contained the property.
	 * @return the property source
	 */
	public PropertySource<?> getPropertySource() {
		return this.propertySource;
	}

	/**
	 * Return the {@link ConfigDataLocation} of the property source or {@code null} if the
	 * source was not loaded from {@link ConfigData}.
	 * @return the config data location or {@code null}
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

	/**
	 * Return the name of the property.
	 * @return the property name
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the origin or the property or {@code null}.
	 * @return the property origin
	 */
	public Origin getOrigin() {
		return this.origin;
	}

	/**
	 * Throw a {@link InactiveConfigDataAccessException} if the given
	 * {@link ConfigDataEnvironmentContributor} contains the property.
	 * @param contributor the contributor to check
	 * @param name the name to check
	 */
	static void throwIfPropertyFound(ConfigDataEnvironmentContributor contributor, ConfigurationPropertyName name) {
		ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
		ConfigurationProperty property = (source != null) ? source.getConfigurationProperty(name) : null;
		if (property != null) {
			PropertySource<?> propertySource = contributor.getPropertySource();
			ConfigDataLocation location = contributor.getLocation();
			throw new InactiveConfigDataAccessException(propertySource, location, name.toString(),
					property.getOrigin());
		}
	}

}
