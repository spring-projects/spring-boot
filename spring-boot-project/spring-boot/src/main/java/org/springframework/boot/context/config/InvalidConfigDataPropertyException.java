/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.env.AbstractEnvironment;

/**
 * Exception thrown if an invalid property is found when processing config data.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class InvalidConfigDataPropertyException extends ConfigDataException {

	private static final Map<ConfigurationPropertyName, ConfigurationPropertyName> ERRORS;
	static {
		Map<ConfigurationPropertyName, ConfigurationPropertyName> errors = new LinkedHashMap<>();
		errors.put(ConfigurationPropertyName.of("spring.profiles"),
				ConfigurationPropertyName.of("spring.config.activate.on-profile"));
		errors.put(ConfigurationPropertyName.of("spring.profiles[0]"),
				ConfigurationPropertyName.of("spring.config.activate.on-profile"));
		ERRORS = Collections.unmodifiableMap(errors);
	}

	private static final Set<ConfigurationPropertyName> PROFILE_SPECIFIC_ERRORS;
	static {
		Set<ConfigurationPropertyName> errors = new LinkedHashSet<>();
		errors.add(Profiles.INCLUDE_PROFILES);
		errors.add(Profiles.INCLUDE_PROFILES.append("[0]"));
		errors.add(ConfigurationPropertyName.of(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME));
		errors.add(ConfigurationPropertyName.of(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME + "[0]"));
		errors.add(ConfigurationPropertyName.of(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME));
		errors.add(ConfigurationPropertyName.of(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME + "[0]"));
		PROFILE_SPECIFIC_ERRORS = Collections.unmodifiableSet(errors);
	}

	private final ConfigurationProperty property;

	private final ConfigurationPropertyName replacement;

	private final ConfigDataResource location;

	InvalidConfigDataPropertyException(ConfigurationProperty property, boolean profileSpecific,
			ConfigurationPropertyName replacement, ConfigDataResource location) {
		super(getMessage(property, profileSpecific, replacement, location), null);
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
	 * Return the {@link ConfigDataResource} of the invalid property or {@code null} if
	 * the source was not loaded from {@link ConfigData}.
	 * @return the config data location or {@code null}
	 */
	public ConfigDataResource getLocation() {
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
	 * Throw an {@link InvalidConfigDataPropertyException} if the given
	 * {@link ConfigDataEnvironmentContributor} contains any invalid property.
	 * @param contributor the contributor to check
	 */
	static void throwIfPropertyFound(ConfigDataEnvironmentContributor contributor) {
		ConfigurationPropertySource propertySource = contributor.getConfigurationPropertySource();
		if (propertySource != null) {
			ERRORS.forEach((name, replacement) -> {
				ConfigurationProperty property = propertySource.getConfigurationProperty(name);
				if (property != null) {
					throw new InvalidConfigDataPropertyException(property, false, replacement,
							contributor.getResource());
				}
			});
			if (contributor.isFromProfileSpecificImport()
					&& !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
				PROFILE_SPECIFIC_ERRORS.forEach((name) -> {
					ConfigurationProperty property = propertySource.getConfigurationProperty(name);
					if (property != null) {
						throw new InvalidConfigDataPropertyException(property, true, null, contributor.getResource());
					}
				});
			}
		}
	}

	private static String getMessage(ConfigurationProperty property, boolean profileSpecific,
			ConfigurationPropertyName replacement, ConfigDataResource location) {
		StringBuilder message = new StringBuilder("Property '");
		message.append(property.getName());
		if (location != null) {
			message.append("' imported from location '");
			message.append(location);
		}
		message.append("' is invalid");
		if (profileSpecific) {
			message.append(" in a profile specific resource");
		}
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
