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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Exception thrown if an invalid property is found when processing config data.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class InvalidConfigDataPropertyException extends ConfigDataException {

	private static final Map<ConfigurationPropertyName, ConfigurationPropertyName> ERROR;

	private static final Map<ConfigurationPropertyName, ConfigurationPropertyName> WARNING;
	static {
		Map<ConfigurationPropertyName, ConfigurationPropertyName> warning = new LinkedHashMap<>();
		warning.put(ConfigurationPropertyName.of("spring.profiles"),
				ConfigurationPropertyName.of("spring.config.activate.on-profile"));
		WARNING = Collections.unmodifiableMap(warning);
		Map<ConfigurationPropertyName, ConfigurationPropertyName> error = new LinkedHashMap<>();
		error.put(ConfigurationPropertyName.of("spring.profiles.include"),
				ConfigurationPropertyName.of("spring.profiles.group"));
		ERROR = Collections.unmodifiableMap(error);
	}

	private final ConfigurationProperty property;

	private final ConfigurationPropertyName replacement;

	private final ConfigDataLocation location;

	InvalidConfigDataPropertyException(ConfigurationProperty property, ConfigurationPropertyName replacement,
			ConfigDataLocation location) {
		super(getMessage(property, replacement, location), null);
		this.property = property;
		this.replacement = replacement;
		this.location = location;
	}

	/**
	 * Return source property that caused the exception.
	 * @return the invalid property
	 */
	public ConfigurationProperty getProperty() {
		return this.property;
	}

	/**
	 * Return the {@link ConfigDataLocation} of the invalid property or {@code null} if
	 * the source was not loaded from {@link ConfigData}.
	 * @return the config data location or {@code null}
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

	/**
	 * Return the replacement property that should be used instead or {@code null} if not
	 * replacement is available.
	 * @return the replacement property name
	 */
	public ConfigurationPropertyName getReplacement() {
		return this.replacement;
	}

	/**
	 * Throw a {@link InvalidConfigDataPropertyException} if the given
	 * {@link ConfigDataEnvironmentContributor} contains any invalid property.
	 * @param logger the logger to use for warnings
	 * @param contributor the contributor to check
	 */
	static void throwOrWarn(Log logger, ConfigDataEnvironmentContributor contributor) {
		ConfigurationPropertySource propertySource = contributor.getConfigurationPropertySource();
		if (propertySource != null) {
			ERROR.forEach((invalid, replacement) -> {
				ConfigurationProperty property = propertySource.getConfigurationProperty(invalid);
				if (property != null) {
					throw new InvalidConfigDataPropertyException(property, replacement, contributor.getLocation());
				}
			});
			WARNING.forEach((invalid, replacement) -> {
				ConfigurationProperty property = propertySource.getConfigurationProperty(invalid);
				if (property != null) {
					logger.warn(getMessage(property, replacement, contributor.getLocation()));
				}
			});
		}
	}

	private static String getMessage(ConfigurationProperty property, ConfigurationPropertyName replacement,
			ConfigDataLocation location) {
		StringBuilder message = new StringBuilder("Property '");
		message.append(property.getName());
		if (location != null) {
			message.append("' imported from location '");
			message.append(location);
		}
		message.append("' is invalid");
		if (replacement != null) {
			message.append(" and should be replaced with '");
			message.append(replacement);
			message.append("'");
		}
		if (property.getOrigin() != null) {
			message.append(" [origin: ");
			message.append(property.getOrigin());
			message.append("]");
		}
		return message.toString();
	}

}
