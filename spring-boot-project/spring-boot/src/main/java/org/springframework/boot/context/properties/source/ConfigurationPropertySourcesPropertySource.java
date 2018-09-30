/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;

/**
 * {@link PropertySource} that exposes {@link ConfigurationPropertySource} instances so
 * that they can be used with a {@link PropertyResolver} or added to the
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertySourcesPropertySource
		extends PropertySource<Iterable<ConfigurationPropertySource>>
		implements OriginLookup<String> {

	ConfigurationPropertySourcesPropertySource(String name,
			Iterable<ConfigurationPropertySource> source) {
		super(name, source);
	}

	@Override
	public Object getProperty(String name) {
		ConfigurationProperty configurationProperty = findConfigurationProperty(name);
		return (configurationProperty != null) ? configurationProperty.getValue() : null;
	}

	@Override
	public Origin getOrigin(String name) {
		return Origin.from(findConfigurationProperty(name));
	}

	private ConfigurationProperty findConfigurationProperty(String name) {
		try {
			return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
		}
		catch (Exception ex) {
			return null;
		}
	}

	private ConfigurationProperty findConfigurationProperty(
			ConfigurationPropertyName name) {
		if (name == null) {
			return null;
		}
		for (ConfigurationPropertySource configurationPropertySource : getSource()) {
			ConfigurationProperty configurationProperty = configurationPropertySource
					.getConfigurationProperty(name);
			if (configurationProperty != null) {
				return configurationProperty;
			}
		}
		return null;
	}

}
