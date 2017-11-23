/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.lang.NonNull;

/**
 * A {@link ConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource source;

	private final ConfigurationPropertyNameAliases aliases;

	AliasedConfigurationPropertySource(@NonNull ConfigurationPropertySource source,
			@NonNull ConfigurationPropertyNameAliases aliases) {
		this.source = source;
		this.aliases = aliases;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			@NonNull ConfigurationPropertyName name) {
		ConfigurationProperty result = getSource().getConfigurationProperty(name);
		if (result == null) {
			ConfigurationPropertyName aliasedName = getAliases().getNameForAlias(name);
			result = getSource().getConfigurationProperty(aliasedName);
		}
		return result;
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(
			@NonNull ConfigurationPropertyName name) {
		ConfigurationPropertyState result = this.source.containsDescendantOf(name);
		if (result != ConfigurationPropertyState.ABSENT) {
			return result;
		}
		for (ConfigurationPropertyName alias : getAliases().getAliases(name)) {
			ConfigurationPropertyState aliasResult = this.source
					.containsDescendantOf(alias);
			if (aliasResult != ConfigurationPropertyState.ABSENT) {
				return aliasResult;
			}
		}
		return ConfigurationPropertyState.ABSENT;
	}

	@Override
	public Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

	protected ConfigurationPropertyNameAliases getAliases() {
		return this.aliases;
	}

}
