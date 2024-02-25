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

	/**
     * Constructs a new ConfigurationPropertySourcesPropertyResolver with the specified MutablePropertySources.
     * 
     * @param propertySources the MutablePropertySources to be used by the resolver
     */
    ConfigurationPropertySourcesPropertyResolver(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.defaultResolver = new DefaultResolver(propertySources);
	}

	/**
     * Checks if the specified key is present in the configuration property sources.
     * 
     * @param key the key to check
     * @return true if the key is present in the configuration property sources, false otherwise
     */
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
					// Ignore
				}
			}
		}
		return this.defaultResolver.containsProperty(key);
	}

	/**
     * Retrieves the value of the property associated with the given key.
     * 
     * @param key the key of the property to retrieve
     * @return the value of the property
     */
    @Override
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	/**
     * Retrieves the value of the property associated with the given key.
     * 
     * @param key               the key of the property to retrieve
     * @param targetValueType   the class representing the desired type of the property value
     * @return                  the value of the property associated with the given key, or null if not found
     * @throws IllegalArgumentException if the key is null or empty
     * @throws ClassCastException       if the value of the property cannot be cast to the specified targetValueType
     */
    @Override
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	/**
     * Retrieves the value of the specified property as a raw string.
     * 
     * @param key the key of the property to retrieve
     * @return the value of the property as a raw string
     */
    @Override
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	/**
     * Retrieves the value of a property specified by the given key.
     * 
     * @param key the key of the property to retrieve
     * @param targetValueType the class representing the desired type of the property value
     * @param resolveNestedPlaceholders a flag indicating whether nested placeholders should be resolved
     * @return the value of the property, or null if the property does not exist
     */
    private <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		Object value = findPropertyValue(key);
		if (value == null) {
			return null;
		}
		if (resolveNestedPlaceholders && value instanceof String string) {
			value = resolveNestedPlaceholders(string);
		}
		return convertValueIfNecessary(value, targetValueType);
	}

	/**
     * Finds the value of a property given its key.
     * 
     * @param key the key of the property to find
     * @return the value of the property, or null if not found
     */
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
					// Ignore
				}
			}
		}
		return this.defaultResolver.getProperty(key, Object.class, false);
	}

	/**
     * Retrieves the attached ConfigurationPropertySourcesPropertySource from the propertySources.
     * 
     * @return The attached ConfigurationPropertySourcesPropertySource, or null if not found.
     */
    private ConfigurationPropertySourcesPropertySource getAttached() {
		ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) ConfigurationPropertySources
			.getAttached(this.propertySources);
		Iterable<ConfigurationPropertySource> attachedSource = (attached != null) ? attached.getSource() : null;
		if ((attachedSource instanceof SpringConfigurationPropertySources springSource)
				&& springSource.isUsingSources(this.propertySources)) {
			return attached;
		}
		return null;
	}

	/**
	 * Default {@link PropertySourcesPropertyResolver} used if
	 * {@link ConfigurationPropertySources} is not attached.
	 */
	static class DefaultResolver extends PropertySourcesPropertyResolver {

		/**
         * Constructs a new DefaultResolver with the specified property sources.
         *
         * @param propertySources the property sources to be used by the resolver
         */
        DefaultResolver(PropertySources propertySources) {
			super(propertySources);
		}

		/**
         * Retrieves the value of the property with the specified key.
         * 
         * @param key the key of the property to retrieve
         * @param targetValueType the class representing the desired type of the property value
         * @param resolveNestedPlaceholders indicates whether nested placeholders should be resolved
         * @return the value of the property with the specified key, or null if the property is not found
         */
        @Override
		public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
			return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
		}

	}

}
