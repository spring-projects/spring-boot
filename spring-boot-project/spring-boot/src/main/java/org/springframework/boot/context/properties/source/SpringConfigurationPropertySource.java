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

package org.springframework.boot.context.properties.source;

import java.util.Map;
import java.util.Random;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.Assert;

/**
 * {@link ConfigurationPropertySource} backed by a non-enumerable Spring
 * {@link PropertySource} or a restricted {@link EnumerablePropertySource} implementation
 * (such as a security restricted {@code systemEnvironment} source). A
 * {@link PropertySource} is adapted with the help of a {@link PropertyMapper} which
 * provides the mapping rules for individual properties.
 * <p>
 * Each {@link ConfigurationPropertySource#getConfigurationProperty
 * getConfigurationProperty} call attempts to
 * {@link PropertyMapper#map(ConfigurationPropertyName) map} the
 * {@link ConfigurationPropertyName} to one or more {@code String} based names. This
 * allows fast property resolution for well-formed property sources.
 * <p>
 * When possible the {@link SpringIterableConfigurationPropertySource} will be used in
 * preference to this implementation since it supports full "relaxed" style resolution.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see #from(PropertySource)
 * @see PropertyMapper
 * @see SpringIterableConfigurationPropertySource
 */
class SpringConfigurationPropertySource implements ConfigurationPropertySource {

	private static final PropertyMapper[] DEFAULT_MAPPERS = { DefaultPropertyMapper.INSTANCE };

	private static final PropertyMapper[] SYSTEM_ENVIRONMENT_MAPPERS = { SystemEnvironmentPropertyMapper.INSTANCE,
			DefaultPropertyMapper.INSTANCE };

	private final PropertySource<?> propertySource;

	private final PropertyMapper[] mappers;

	/**
	 * Create a new {@link SpringConfigurationPropertySource} implementation.
	 * @param propertySource the source property source
	 * @param mappers the property mappers
	 */
	SpringConfigurationPropertySource(PropertySource<?> propertySource, PropertyMapper... mappers) {
		Assert.notNull(propertySource, "PropertySource must not be null");
		Assert.isTrue(mappers.length > 0, "Mappers must contain at least one item");
		this.propertySource = propertySource;
		this.mappers = mappers;
	}

	/**
	 * Retrieves the configuration property with the given name from the
	 * SpringConfigurationPropertySource.
	 * @param name the name of the configuration property
	 * @return the ConfigurationProperty object representing the configuration property,
	 * or null if not found
	 */
	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		if (name == null) {
			return null;
		}
		for (PropertyMapper mapper : this.mappers) {
			try {
				for (String candidate : mapper.map(name)) {
					Object value = getPropertySource().getProperty(candidate);
					if (value != null) {
						Origin origin = PropertySourceOrigin.get(this.propertySource, candidate);
						return ConfigurationProperty.of(this, name, value, origin);
					}
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Checks if the configuration property source contains a descendant of the given
	 * configuration property name.
	 * @param name the configuration property name to check for descendants
	 * @return the state of the configuration property (CONTAINS, ABSENT, UNKNOWN)
	 */
	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		PropertySource<?> source = getPropertySource();
		Object underlyingSource = source.getSource();
		if (underlyingSource instanceof Random) {
			return containsDescendantOfForRandom("random", name);
		}
		if (underlyingSource instanceof PropertySource<?> underlyingPropertySource
				&& underlyingPropertySource.getSource() instanceof Random) {
			// Assume wrapped random sources use the source name as the prefix
			return containsDescendantOfForRandom(source.getName(), name);
		}
		return ConfigurationPropertyState.UNKNOWN;
	}

	/**
	 * Checks if the given ConfigurationPropertyName contains a descendant of the
	 * specified prefix.
	 * @param prefix the prefix to check against
	 * @param name the ConfigurationPropertyName to check
	 * @return the ConfigurationPropertyState indicating if the descendant is present or
	 * absent
	 */
	private static ConfigurationPropertyState containsDescendantOfForRandom(String prefix,
			ConfigurationPropertyName name) {
		if (name.getNumberOfElements() > 1 && name.getElement(0, Form.DASHED).equals(prefix)) {
			return ConfigurationPropertyState.PRESENT;
		}
		return ConfigurationPropertyState.ABSENT;
	}

	/**
	 * Returns the underlying source of the configuration property.
	 * @return the underlying source of the configuration property
	 */
	@Override
	public Object getUnderlyingSource() {
		return this.propertySource;
	}

	/**
	 * Returns the property source associated with this SpringConfigurationPropertySource
	 * instance.
	 * @return the property source
	 */
	protected PropertySource<?> getPropertySource() {
		return this.propertySource;
	}

	/**
	 * Returns the array of PropertyMapper objects used by this
	 * SpringConfigurationPropertySource.
	 * @return the array of PropertyMapper objects
	 */
	protected final PropertyMapper[] getMappers() {
		return this.mappers;
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		return this.propertySource.toString();
	}

	/**
	 * Create a new {@link SpringConfigurationPropertySource} for the specified
	 * {@link PropertySource}.
	 * @param source the source Spring {@link PropertySource}
	 * @return a {@link SpringConfigurationPropertySource} or
	 * {@link SpringIterableConfigurationPropertySource} instance
	 */
	static SpringConfigurationPropertySource from(PropertySource<?> source) {
		Assert.notNull(source, "Source must not be null");
		PropertyMapper[] mappers = getPropertyMappers(source);
		if (isFullEnumerable(source)) {
			return new SpringIterableConfigurationPropertySource((EnumerablePropertySource<?>) source, mappers);
		}
		return new SpringConfigurationPropertySource(source, mappers);
	}

	/**
	 * Returns an array of PropertyMapper objects based on the given PropertySource. If
	 * the PropertySource is an instance of SystemEnvironmentPropertySource and has a
	 * system environment name, it returns the SYSTEM_ENVIRONMENT_MAPPERS array.
	 * Otherwise, it returns the DEFAULT_MAPPERS array.
	 * @param source the PropertySource to get the PropertyMappers from
	 * @return an array of PropertyMapper objects based on the given PropertySource
	 */
	private static PropertyMapper[] getPropertyMappers(PropertySource<?> source) {
		if (source instanceof SystemEnvironmentPropertySource && hasSystemEnvironmentName(source)) {
			return SYSTEM_ENVIRONMENT_MAPPERS;
		}
		return DEFAULT_MAPPERS;
	}

	/**
	 * Checks if the given property source has the name of the system environment property
	 * source.
	 * @param source the property source to check
	 * @return true if the property source has the name of the system environment property
	 * source, false otherwise
	 */
	private static boolean hasSystemEnvironmentName(PropertySource<?> source) {
		String name = source.getName();
		return StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name)
				|| name.endsWith("-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
	}

	/**
	 * Checks if the given PropertySource is fully enumerable.
	 * @param source the PropertySource to check
	 * @return true if the PropertySource is fully enumerable, false otherwise
	 */
	private static boolean isFullEnumerable(PropertySource<?> source) {
		PropertySource<?> rootSource = getRootSource(source);
		if (rootSource.getSource() instanceof Map) {
			// Check we're not security restricted
			try {
				((Map<?, ?>) rootSource.getSource()).size();
			}
			catch (UnsupportedOperationException ex) {
				return false;
			}
		}
		return (source instanceof EnumerablePropertySource);
	}

	/**
	 * Retrieves the root property source from the given property source.
	 * @param source the property source to retrieve the root from
	 * @return the root property source
	 */
	private static PropertySource<?> getRootSource(PropertySource<?> source) {
		while (source.getSource() != null && source.getSource() instanceof PropertySource) {
			source = (PropertySource<?>) source.getSource();
		}
		return source;
	}

}
