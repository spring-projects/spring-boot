/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

import static java.lang.String.format;

/**
 * {@link PropertyResolver} that attempts to resolve values using {@link RelaxedNames}.
 * 
 * @author Phillip Webb
 * @see RelaxedNames
 */
public class RelaxedPropertyResolver implements PropertyResolver {

	private final PropertyResolver resolver;

	private final String prefix;

	public RelaxedPropertyResolver(PropertyResolver resolver) {
		this(resolver, null);
	}

	public RelaxedPropertyResolver(PropertyResolver resolver, String prefix) {
		Assert.notNull(resolver, "PropertyResolver must not be null");
		this.resolver = resolver;
		this.prefix = (prefix == null ? "" : prefix);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return getRequiredProperty(key, String.class);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType)
			throws IllegalStateException {
		T value = getProperty(key, targetType);
		Assert.state(value != null, format("required key [%s] not found", key));
		return value;
	}

	@Override
	public String getProperty(String key) {
		return getProperty(key, String.class, null);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return getProperty(key, String.class, defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType) {
		return getProperty(key, targetType, null);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		for (String relaxedKey : new RelaxedNames(key)) {
			if (this.resolver.containsProperty(this.prefix + relaxedKey)) {
				return this.resolver.getProperty(this.prefix + relaxedKey, targetType,
						defaultValue);
			}
		}
		return defaultValue;
	}

	@Override
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetType) {
		for (String relaxedKey : new RelaxedNames(key)) {
			if (this.resolver.containsProperty(this.prefix + relaxedKey)) {
				return this.resolver.getPropertyAsClass(this.prefix + relaxedKey,
						targetType);
			}
		}
		return null;
	}

	@Override
	public boolean containsProperty(String key) {
		for (String relaxedKey : new RelaxedNames(key)) {
			if (this.resolver.containsProperty(this.prefix + relaxedKey)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String resolvePlaceholders(String text) {
		throw new UnsupportedOperationException(
				"Unable to resolve placeholders with relaxed properties");
	}

	@Override
	public String resolveRequiredPlaceholders(String text)
			throws IllegalArgumentException {
		throw new UnsupportedOperationException(
				"Unable to resolve placeholders with relaxed properties");
	}

	/**
	 * Return a Map of all values from all underlying properties that start with the
	 * specified key. NOTE: this method can only be used in the underlying resolver is a
	 * {@link ConfigurableEnvironment}.
	 * @param keyPrefix the key prefix used to filter results
	 * @return a map of all sub properties starting with the specified key prefix.
	 * @see #getSubProperties(PropertySources, RelaxedNames)
	 * @see #getSubProperties(PropertySources, String, RelaxedNames)
	 */
	public Map<String, Object> getSubProperties(String keyPrefix) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, this.resolver,
				"SubProperties not available.");
		ConfigurableEnvironment env = (ConfigurableEnvironment) this.resolver;
		return getSubProperties(env.getPropertySources(), this.prefix, new RelaxedNames(
				keyPrefix));
	}

	/**
	 * Return a Map of all values from the specified {@link PropertySources} that start
	 * with a particular key.
	 * @param propertySources the property sources to scan
	 * @param keyPrefix the key prefixes to test
	 * @return a map of all sub properties starting with the specified key prefixes.
	 * @see #getSubProperties(PropertySources, String, RelaxedNames)
	 */
	public static Map<String, Object> getSubProperties(PropertySources propertySources,
			RelaxedNames keyPrefix) {
		return getSubProperties(propertySources, null, keyPrefix);
	}

	/**
	 * Return a Map of all values from the specified {@link PropertySources} that start
	 * with a particular key.
	 * @param propertySources the property sources to scan
	 * @param rootPrefix a root prefix to be prepended to the keyPrefex (can be
	 * {@code null})
	 * @param keyPrefix the key prefixes to test
	 * @return a map of all sub properties starting with the specified key prefixes.
	 * @see #getSubProperties(PropertySources, String, RelaxedNames)
	 */
	public static Map<String, Object> getSubProperties(PropertySources propertySources,
			String rootPrefix, RelaxedNames keyPrefix) {
		Map<String, Object> subProperties = new LinkedHashMap<String, Object>();
		for (PropertySource<?> source : propertySources) {
			if (source instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource<?>) source)
						.getPropertyNames()) {
					String key = getSubKey(name, rootPrefix, keyPrefix);
					if (key != null) {
						subProperties.put(key, source.getProperty(name));
					}
				}
			}
		}
		return Collections.unmodifiableMap(subProperties);
	}

	private static String getSubKey(String name, String rootPrefix, RelaxedNames keyPrefix) {
		rootPrefix = (rootPrefix == null ? "" : rootPrefix);
		for (String candidateKeyPrefix : keyPrefix) {
			if (name.startsWith(rootPrefix + candidateKeyPrefix)) {
				return name.substring((rootPrefix + candidateKeyPrefix).length());
			}
		}
		return null;
	}

}
