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

import org.springframework.util.Assert;

/**
 * A {@link ConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final ConfigurationPropertyNameAliases aliases;

	/**
	 * Constructs a new AliasedConfigurationPropertySource with the given source and
	 * aliases.
	 * @param source the underlying ConfigurationPropertySource (must not be null)
	 * @param aliases the ConfigurationPropertyNameAliases to be used for alias resolution
	 * (must not be null)
	 * @throws IllegalArgumentException if either source or aliases is null
	 */
	AliasedConfigurationPropertySource(ConfigurationPropertySource source, ConfigurationPropertyNameAliases aliases) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(aliases, "Aliases must not be null");
		this.source = source;
		this.aliases = aliases;
	}

	/**
	 * Retrieves the configuration property with the given name from the source.
	 * @param name the name of the configuration property to retrieve
	 * @return the configuration property with the given name, or null if not found
	 * @throws IllegalArgumentException if the name is null
	 */
	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		ConfigurationProperty result = getSource().getConfigurationProperty(name);
		if (result == null) {
			ConfigurationPropertyName aliasedName = getAliases().getNameForAlias(name);
			result = getSource().getConfigurationProperty(aliasedName);
		}
		return result;
	}

	/**
	 * Checks if the configuration property source contains a descendant of the given
	 * name.
	 * @param name the name of the configuration property
	 * @return the state of the configuration property
	 * @throws IllegalArgumentException if the name is null
	 */
	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result != ConfigurationPropertyState.ABSENT) {
			return result;
		}
		for (ConfigurationPropertyName alias : getAliases().getAliases(name)) {
			ConfigurationPropertyState aliasResult = this.source.containsDescendantOf(alias);
			if (aliasResult != ConfigurationPropertyState.ABSENT) {
				return aliasResult;
			}
		}
		for (ConfigurationPropertyName from : getAliases()) {
			for (ConfigurationPropertyName alias : getAliases().getAliases(from)) {
				if (name.isAncestorOf(alias)) {
					if (this.source.getConfigurationProperty(from) != null) {
						return ConfigurationPropertyState.PRESENT;
					}
				}
			}
		}
		return ConfigurationPropertyState.ABSENT;
	}

	/**
	 * Returns the underlying source of this AliasedConfigurationPropertySource.
	 * @return the underlying source of this AliasedConfigurationPropertySource
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
	 * Returns the aliases associated with this AliasedConfigurationPropertySource.
	 * @return the aliases associated with this AliasedConfigurationPropertySource
	 */
	protected ConfigurationPropertyNameAliases getAliases() {
		return this.aliases;
	}

}
