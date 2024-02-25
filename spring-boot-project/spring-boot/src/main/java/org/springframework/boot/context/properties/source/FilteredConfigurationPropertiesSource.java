/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * A filtered {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final Predicate<ConfigurationPropertyName> filter;

	/**
	 * Constructs a new FilteredConfigurationPropertiesSource with the specified source
	 * and filter.
	 * @param source the ConfigurationPropertySource to be filtered (must not be null)
	 * @param filter the Predicate used to filter the ConfigurationPropertyNames (must not
	 * be null)
	 * @throws IllegalArgumentException if either source or filter is null
	 */
	FilteredConfigurationPropertiesSource(ConfigurationPropertySource source,
			Predicate<ConfigurationPropertyName> filter) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.source = source;
		this.filter = filter;
	}

	/**
	 * Retrieves the configuration property with the specified name from the filtered
	 * configuration properties source.
	 * @param name the name of the configuration property to retrieve
	 * @return the configuration property with the specified name, or null if it does not
	 * exist
	 */
	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		boolean filtered = getFilter().test(name);
		return filtered ? getSource().getConfigurationProperty(name) : null;
	}

	/**
	 * Determines if the configuration property source contains a descendant of the
	 * specified property name.
	 * @param name the name of the property to check for descendants
	 * @return the state of the property: - {@code ConfigurationPropertyState.PRESENT} if
	 * the property or a descendant is present -
	 * {@code ConfigurationPropertyState.UNKNOWN} if a contained descendant may be
	 * filtered - {@code ConfigurationPropertyState.ABSENT} if the property or a
	 * descendant is absent
	 */
	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result == ConfigurationPropertyState.PRESENT) {
			// We can't be sure a contained descendant won't be filtered
			return ConfigurationPropertyState.UNKNOWN;
		}
		return result;
	}

	/**
	 * Returns the underlying source of the FilteredConfigurationPropertiesSource.
	 * @return the underlying source of the FilteredConfigurationPropertiesSource
	 */
	@Override
	public Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	/**
	 * Returns the source of the configuration property.
	 * @return the source of the configuration property
	 */
	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

	/**
	 * Returns the filter used to determine which configuration property names should be
	 * included in the filtered configuration properties source.
	 * @return the filter used to determine which configuration property names should be
	 * included
	 */
	protected Predicate<ConfigurationPropertyName> getFilter() {
		return this.filter;
	}

	/**
	 * Returns a string representation of the object. The string representation consists
	 * of the source object's string representation followed by the text " (filtered)".
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		return this.source.toString() + " (filtered)";
	}

}
