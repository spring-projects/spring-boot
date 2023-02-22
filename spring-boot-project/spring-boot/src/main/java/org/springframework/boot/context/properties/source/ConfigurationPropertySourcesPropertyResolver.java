/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

/**
 * Alternative {@link PropertySourcesPropertyResolver} implementation that recognizes
 * {@link ConfigurationPropertySourcesPropertySource} and saves duplicate calls to the
 * underlying sources if the name is a value {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesPropertyResolver extends AbstractPropertyResolver {

	private final MutablePropertySources propertySources;

	private final DefaultResolver defaultResolver;

	ConfigurationPropertySourcesPropertyResolver(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.defaultResolver = new DefaultResolver(propertySources);
	}

	@Override
	public boolean containsProperty(String key) {
		ConfigurationPropertySourcesPropertySource attached = getAttached();
		if (attached != null) {
			ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
			if (name != null) {
				try {
					return attached.findConfigurationProperty(name) != null;
				}
				catch (Exception ex) {
				}
			}
		}
		return this.defaultResolver.containsProperty(key);
	}

	@Override
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	private <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		Object value = findPropertyValue(key);
		if (value == null) {
			return null;
		}
		if (resolveNestedPlaceholders && value instanceof String) {
			value = resolveNestedPlaceholders((String) value);
		}
		return convertValueIfNecessary(value, targetValueType);
	}

	private Object findPropertyValue(String key) {
		ConfigurationPropertySourcesPropertySource attached = getAttached();
		if (attached != null) {
			ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
			if (name != null) {
				try {
					ConfigurationProperty configurationProperty = attached.findConfigurationProperty(name);
					return (configurationProperty != null) ? configurationProperty.getValue() : null;
				}
				catch (Exception ex) {
				}
			}
		}
		return this.defaultResolver.getProperty(key, Object.class, false);
	}

	private ConfigurationPropertySourcesPropertySource getAttached() {
		ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) ConfigurationPropertySources
			.getAttached(this.propertySources);
		Iterable<ConfigurationPropertySource> attachedSource = (attached != null) ? attached.getSource() : null;
		if ((attachedSource instanceof SpringConfigurationPropertySources)
				&& ((SpringConfigurationPropertySources) attachedSource).isUsingSources(this.propertySources)) {
			return attached;
		}
		return null;
	}

	/**
	 * Default {@link PropertySourcesPropertyResolver} used if
	 * {@link ConfigurationPropertySources} is not attached.
	 */
	static class DefaultResolver extends PropertySourcesPropertyResolver {

		DefaultResolver(PropertySources propertySources) {
			super(propertySources);
		}

		@Override
		public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
			return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
		}

	}

}
