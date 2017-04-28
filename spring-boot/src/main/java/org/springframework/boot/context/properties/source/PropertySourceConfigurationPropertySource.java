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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link ConfigurationPropertySource} backed by a Spring {@link PropertySource}. Provides
 * support for {@link EnumerablePropertySource} when possible but can also be used to
 * non-enumerable property sources or restricted {@link EnumerablePropertySource}
 * implementation (such as a security restricted {@code systemEnvironment} source). A
 * {@link PropertySource} is adapted with the help of a {@link PropertyMapper} which
 * provides the mapping rules for individual properties.
 * <p>
 * Each
 * {@link ConfigurationPropertySource#getConfigurationProperty(ConfigurationPropertyName)
 * getValue} call initially attempts to
 * {@link PropertyMapper#map(PropertySource, ConfigurationPropertyName) map} the
 * {@link ConfigurationPropertyName} to one or more {@code String} based names. This
 * allows fast property resolution for well formed property sources and allows the adapter
 * to work with non {@link EnumerablePropertySource enumerable property sources}.
 * <p>
 * If direct {@link ConfigurationPropertyName} to {@code String} mapping is unsuccessful a
 * brute force approach is taken by {@link EnumerablePropertySource#getPropertyNames()
 * enumerating} known {@code String} {@link PropertySource} names, mapping them to one or
 * more {@link ConfigurationPropertyName} and checking for
 * {@link PropertyMapping#isApplicable(ConfigurationPropertyName) applicability}. The
 * enumeration approach supports property sources where it isn't practical to guess all
 * direct mapping combinations.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 */
class PropertySourceConfigurationPropertySource implements ConfigurationPropertySource {

	private final PropertySource<?> propertySource;

	private final PropertyMapper mapper;

	private volatile Object cacheKey;

	private volatile Cache cache;

	/**
	 * Create a new {@link PropertySourceConfigurationPropertySource} implementation.
	 * @param propertySource the source property source
	 * @param mapper the property mapper
	 */
	PropertySourceConfigurationPropertySource(PropertySource<?> propertySource,
			PropertyMapper mapper) {
		Assert.notNull(propertySource, "PropertySource must not be null");
		Assert.notNull(mapper, "Mapper must not be null");
		this.propertySource = propertySource;
		this.mapper = new ExceptionSwallowingPropertyMapper(mapper);
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		ConfigurationProperty configurationProperty = findDirectly(name);
		if (configurationProperty == null) {
			configurationProperty = findByEnumeration(name);
		}
		return configurationProperty;
	}

	private ConfigurationProperty findDirectly(ConfigurationPropertyName name) {
		List<PropertyMapping> mappings = this.mapper.map(this.propertySource, name);
		return find(mappings, name);
	}

	private ConfigurationProperty findByEnumeration(ConfigurationPropertyName name) {
		List<PropertyMapping> mappings = getPropertyMappings();
		return find(mappings, name);
	}

	private ConfigurationProperty find(List<PropertyMapping> mappings,
			ConfigurationPropertyName name) {
		// Use for-loops rather than streams since this method is called often
		for (PropertyMapping mapping : mappings) {
			if (mapping.isApplicable(name)) {
				ConfigurationProperty property = find(mapping);
				if (property != null) {
					return property;
				}
			}
		}
		return null;
	}

	private ConfigurationProperty find(PropertyMapping mapping) {
		String propertySourceName = mapping.getPropertySourceName();
		Object value = this.propertySource.getProperty(propertySourceName);
		if (value == null) {
			return null;
		}
		value = mapping.getValueExtractor().apply(value);
		ConfigurationPropertyName configurationPropertyName = mapping
				.getConfigurationPropertyName();
		Origin origin = PropertySourceOrigin.get(this.propertySource, propertySourceName);
		return ConfigurationProperty.of(configurationPropertyName, value, origin);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getConfigurationPropertyNames().stream();
	}

	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return getConfigurationPropertyNames().iterator();
	}

	private List<ConfigurationPropertyName> getConfigurationPropertyNames() {
		Cache cache = getCache();
		List<ConfigurationPropertyName> names = (cache != null ? cache.getNames() : null);
		if (names != null) {
			return names;
		}
		List<PropertyMapping> mappings = getPropertyMappings();
		names = new ArrayList<>(mappings.size());
		for (PropertyMapping mapping : mappings) {
			names.add(mapping.getConfigurationPropertyName());
		}
		names = Collections.unmodifiableList(names);
		if (cache != null) {
			cache.setNames(names);
		}
		return names;
	}

	private List<PropertyMapping> getPropertyMappings() {
		if (!(this.propertySource instanceof EnumerablePropertySource)) {
			return Collections.emptyList();
		}
		Cache cache = getCache();
		List<PropertyMapping> mappings = (cache != null ? cache.getMappings() : null);
		if (mappings != null) {
			return mappings;
		}
		String[] names = ((EnumerablePropertySource<?>) this.propertySource)
				.getPropertyNames();
		mappings = new ArrayList<>(names.length);
		for (String name : names) {
			mappings.addAll(this.mapper.map(this.propertySource, name));
		}
		mappings = Collections.unmodifiableList(mappings);
		if (cache != null) {
			cache.setMappings(mappings);
		}
		return mappings;
	}

	private Cache getCache() {
		Object cacheKey = getCacheKey();
		if (cacheKey == null) {
			return null;
		}
		if (ObjectUtils.nullSafeEquals(cacheKey, this.cacheKey)) {
			return this.cache;
		}
		this.cache = new Cache();
		this.cacheKey = cacheKey;
		return this.cache;
	}

	private Object getCacheKey() {
		if (this.propertySource instanceof MapPropertySource) {
			return ((MapPropertySource) this.propertySource).getSource().keySet();
		}
		if (this.propertySource instanceof EnumerablePropertySource) {
			return ((EnumerablePropertySource<?>) this.propertySource).getPropertyNames();
		}
		return null;
	}

	/**
	 * {@link PropertyMapper} that swallows exceptions when the mapping fails.
	 */
	private static class ExceptionSwallowingPropertyMapper implements PropertyMapper {

		private final PropertyMapper mapper;

		ExceptionSwallowingPropertyMapper(PropertyMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				ConfigurationPropertyName configurationPropertyName) {
			try {
				return this.mapper.map(propertySource, configurationPropertyName);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				String propertySourceName) {
			try {
				return this.mapper.map(propertySource, propertySourceName);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

	}

	private static class Cache {

		private List<ConfigurationPropertyName> names;

		private List<PropertyMapping> mappings;

		public List<ConfigurationPropertyName> getNames() {
			return this.names;
		}

		public void setNames(List<ConfigurationPropertyName> names) {
			this.names = names;
		}

		public List<PropertyMapping> getMappings() {
			return this.mappings;
		}

		public void setMappings(List<PropertyMapping> mappings) {
			this.mappings = mappings;
		}

	}

}
