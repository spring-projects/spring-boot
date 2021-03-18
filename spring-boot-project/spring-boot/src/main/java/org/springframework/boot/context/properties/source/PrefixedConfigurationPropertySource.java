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

	PrefixedConfigurationPropertySource(ConfigurationPropertySource source, String prefix) {
		Assert.notNull(source, "Source must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		this.source = source;
		this.prefix = ConfigurationPropertyName.of(prefix);
	}

	protected final ConfigurationPropertyName getPrefix() {
		return this.prefix;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		ConfigurationProperty configurationProperty = this.source.getConfigurationProperty(getPrefixedName(name));
		if (configurationProperty == null) {
			return null;
		}
		return ConfigurationProperty.of(name, configurationProperty.getValue(), configurationProperty.getOrigin());
	}

	private ConfigurationPropertyName getPrefixedName(ConfigurationPropertyName name) {
		return this.prefix.append(name);
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		return this.source.containsDescendantOf(getPrefixedName(name));
	}

	@Override
	public Object getUnderlyingSource() {
		return this.source.getUnderlyingSource();
	}

	protected ConfigurationPropertySource getSource() {
		return this.source;
	}

}
