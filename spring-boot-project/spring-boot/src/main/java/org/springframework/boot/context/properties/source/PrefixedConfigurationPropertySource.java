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

package org.springframework.boot.context.properties.source;

import org.springframework.util.Assert;

/**
 * A {@link ConfigurationPropertySource} supporting a prefix.
 *
 * @author Madhura Bhave
 */
class PrefixedConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final ConfigurationPropertyName prefix;

	/**
	 * Constructs a new PrefixedConfigurationPropertySource with the given source and
	 * prefix.
	 * @param source the underlying ConfigurationPropertySource (must not be null)
	 * @param prefix the prefix to be applied to property names (must not be empty)
	 * @throws IllegalArgumentException if the source is null or the prefix is empty
	 */
	PrefixedConfigurationPropertySource(ConfigurationPropertySource source, String prefix) {
		Assert.notNull(source, "Source must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		this.source = source;
		this.prefix = ConfigurationPropertyName.of(prefix);
	}

	/**
	 * Returns the prefix associated with this PrefixedConfigurationPropertySource.
	 * @return the prefix associated with this PrefixedConfigurationPropertySource
	 */
	protected final ConfigurationPropertyName getPrefix() {
		return this.prefix;
	}

	/**
	 * Retrieves the configuration property with the given name from the source.
	 * @param name The name of the configuration property to retrieve.
	 * @return The configuration property with the given name, or null if not found.
	 */
	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		ConfigurationProperty configurationProperty = this.source.getConfigurationProperty(getPrefixedName(name));
		if (configurationProperty == null) {
			return null;
		}
		return ConfigurationProperty.of(configurationProperty.getSource(), name, configurationProperty.getValue(),
				configurationProperty.getOrigin());
	}

	/**
	 * Returns the prefixed name of the given configuration property name.
	 * @param name the configuration property name
	 * @return the prefixed configuration property name
	 */
	private ConfigurationPropertyName getPrefixedName(ConfigurationPropertyName name) {
		return this.prefix.append(name);
	}

	/**
	 * Checks if the PrefixedConfigurationPropertySource contains a descendant of the
	 * specified ConfigurationPropertyName.
	 * @param name the ConfigurationPropertyName to check for descendants
	 * @return the ConfigurationPropertyState indicating if the
	 * PrefixedConfigurationPropertySource contains a descendant of the specified
	 * ConfigurationPropertyName
	 */
	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		return this.source.containsDescendantOf(getPrefixedName(name));
	}

	/**
	 * Returns the underlying source of the PrefixedConfigurationPropertySource.
	 * @return the underlying source of the PrefixedConfigurationPropertySource
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

}
