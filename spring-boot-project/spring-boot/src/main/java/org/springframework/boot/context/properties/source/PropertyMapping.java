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

import org.springframework.core.env.PropertySource;

/**
 * Details a mapping between a {@link PropertySource} item and a
 * {@link ConfigurationPropertySource} item.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see SpringConfigurationPropertySource
 */
class PropertyMapping {

	private final String propertySourceName;

	private final ConfigurationPropertyName configurationPropertyName;

	/**
	 * Create a new {@link PropertyMapper} instance.
	 * @param propertySourceName the {@link PropertySource} name
	 * @param configurationPropertyName the {@link ConfigurationPropertySource}
	 * {@link ConfigurationPropertyName}
	 */
	PropertyMapping(String propertySourceName, ConfigurationPropertyName configurationPropertyName) {
		this.propertySourceName = propertySourceName;
		this.configurationPropertyName = configurationPropertyName;
	}

	/**
	 * Return the mapped {@link PropertySource} name.
	 * @return the property source name (never {@code null})
	 */
	String getPropertySourceName() {
		return this.propertySourceName;
	}

	/**
	 * Return the mapped {@link ConfigurationPropertySource}
	 * {@link ConfigurationPropertyName}.
	 * @return the configuration property source name (never {@code null})
	 */
	ConfigurationPropertyName getConfigurationPropertyName() {
		return this.configurationPropertyName;
	}

	/**
	 * Return if this mapping is applicable for the given
	 * {@link ConfigurationPropertyName}.
	 * @param name the name to check
	 * @return if the mapping is applicable
	 */
	boolean isApplicable(ConfigurationPropertyName name) {
		return this.configurationPropertyName.equals(name);
	}

}
