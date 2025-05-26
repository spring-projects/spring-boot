/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link ConfigurationPropertySource} backed by an {@link EnumerablePropertySource}.
 * Extends {@link SpringConfigurationPropertySource} with full "relaxed" mapping support.
 * In order to use this adapter the underlying {@link PropertySource} must be fully
 * enumerable. A security restricted {@link SystemEnvironmentPropertySource} cannot be
 * adapted.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 */
class SpringIterableConfigurationPropertySource extends SpringConfigurationPropertySource
		implements IterableConfigurationPropertySource, CachingConfigurationPropertySource {

	private final BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> ancestorOfCheck;

	private final SoftReferenceConfigurationPropertyCache<Cache> cache;

	private volatile ConfigurationPropertyName[] configurationPropertyNames;

	private final Map<ConfigurationPropertyName, ConfigurationPropertyState> containsDescendantOfCache;

	SpringIterableConfigurationPropertySource(EnumerablePropertySource<?> propertySource,
			boolean systemEnvironmentSource, PropertyMapper... mappers) {
		super(propertySource, systemEnvironmentSource, mappers);
		assertEnumerablePropertySource();
		boolean immutable = isImmutablePropertySource();
		this.ancestorOfCheck = getAncestorOfCheck(mappers);
		this.cache = new SoftReferenceConfigurationPropertyCache<>(immutable);
		this.containsDescendantOfCache = (!systemEnvironmentSource) ? null : new ConcurrentReferenceHashMap<>();
	}

	private BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck(
			PropertyMapper[] mappers) {
		BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> ancestorOfCheck = mappers[0]
			.getAncestorOfCheck();
		for (int i = 1; i < mappers.length; i++) {
			ancestorOfCheck = ancestorOfCheck.or(mappers[i].getAncestorOfCheck());
		}
		return ancestorOfCheck;
	}

	private void assertEnumerablePropertySource() {
		if (getPropertySource() instanceof MapPropertySource mapSource) {
			try {
				mapSource.getSource().size();
			}
			catch (UnsupportedOperationException ex) {
				throw new IllegalArgumentException("PropertySource must be fully enumerable");
			}
		}
	}

	@Override
	public ConfigurationPropertyCaching getCaching() {
		return this.cache;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		if (name == null) {
			return null;
		}
		ConfigurationProperty configurationProperty = super.getConfigurationProperty(name);
		if (configurationProperty != null) {
			return configurationProperty;
		}
		for (String candidate : getCache().getMapped(name)) {
			Object value = getPropertySourceProperty(candidate);
			if (value != null) {
				Origin origin = PropertySourceOrigin.get(getPropertySource(), candidate);
				return ConfigurationProperty.of(this, name, value, origin);
			}
		}
		return null;
	}

	@Override
	protected Object getSystemEnvironmentProperty(Map<String, Object> systemEnvironment, String name) {
		return getCache().getSystemEnvironmentProperty(name);
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		ConfigurationPropertyName[] names = getConfigurationPropertyNames();
		return Arrays.stream(names).filter(Objects::nonNull);
	}

	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return new ConfigurationPropertyNamesIterator(getConfigurationPropertyNames());
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState result = super.containsDescendantOf(name);
		if (result != ConfigurationPropertyState.UNKNOWN) {
			return result;
		}
		if (this.ancestorOfCheck == PropertyMapper.DEFAULT_ANCESTOR_OF_CHECK) {
			Set<ConfigurationPropertyName> descendants = getCache().getDescendants();
			if (descendants != null) {
				if (name.isEmpty() && !descendants.isEmpty()) {
					return ConfigurationPropertyState.PRESENT;
				}
				return !descendants.contains(name) ? ConfigurationPropertyState.ABSENT
						: ConfigurationPropertyState.PRESENT;
			}
		}
		result = (this.containsDescendantOfCache != null) ? this.containsDescendantOfCache.get(name) : null;
		if (result == null) {
			result = (!ancestorOfCheck(name)) ? ConfigurationPropertyState.ABSENT : ConfigurationPropertyState.PRESENT;
			if (this.containsDescendantOfCache != null) {
				this.containsDescendantOfCache.put(name, result);
			}
		}
		return result;
	}

	private boolean ancestorOfCheck(ConfigurationPropertyName name) {
		ConfigurationPropertyName[] candidates = getConfigurationPropertyNames();
		for (ConfigurationPropertyName candidate : candidates) {
			if (candidate != null && this.ancestorOfCheck.test(name, candidate)) {
				return true;
			}
		}
		return false;
	}

	private ConfigurationPropertyName[] getConfigurationPropertyNames() {
		if (!isImmutablePropertySource()) {
			return getCache().getConfigurationPropertyNames(getPropertySource().getPropertyNames());
		}
		ConfigurationPropertyName[] configurationPropertyNames = this.configurationPropertyNames;
		if (configurationPropertyNames == null) {
			configurationPropertyNames = getCache()
				.getConfigurationPropertyNames(getPropertySource().getPropertyNames());
			this.configurationPropertyNames = configurationPropertyNames;
		}
		return configurationPropertyNames;
	}

	private Cache getCache() {
		return this.cache.get(this::createCache, this::updateCache);
	}

	private Cache createCache() {
		boolean immutable = isImmutablePropertySource();
		boolean captureDescendants = this.ancestorOfCheck == PropertyMapper.DEFAULT_ANCESTOR_OF_CHECK;
		return new Cache(getMappers(), immutable, captureDescendants, isSystemEnvironmentSource());
	}

	private Cache updateCache(Cache cache) {
		cache.update(getPropertySource());
		return cache;
	}

	private boolean isImmutablePropertySource() {
		EnumerablePropertySource<?> source = getPropertySource();
		if (source instanceof OriginLookup<?> originLookup) {
			return originLookup.isImmutable();
		}
		if (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(source.getName())) {
			return source.getSource() == System.getenv();
		}
		return false;
	}

	@Override
	protected EnumerablePropertySource<?> getPropertySource() {
		return (EnumerablePropertySource<?>) super.getPropertySource();
	}

	private static class Cache {

		private static final ConfigurationPropertyName[] EMPTY_NAMES_ARRAY = {};

		private final PropertyMapper[] mappers;

		private final boolean immutable;

		private final boolean captureDescendants;

		private final boolean systemEnvironmentSource;

		private volatile Data data;

		Cache(PropertyMapper[] mappers, boolean immutable, boolean captureDescendants,
				boolean systemEnvironmentSource) {
			this.mappers = mappers;
			this.immutable = immutable;
			this.captureDescendants = captureDescendants;
			this.systemEnvironmentSource = systemEnvironmentSource;
		}

		void update(EnumerablePropertySource<?> propertySource) {
			if (this.data == null || !this.immutable) {
				int count = 0;
				while (true) {
					try {
						tryUpdate(propertySource);
						return;
					}
					catch (ConcurrentModificationException ex) {
						if (count++ > 10) {
							throw ex;
						}
					}
				}
			}
		}

		private void tryUpdate(EnumerablePropertySource<?> propertySource) {
			Data data = this.data;
			String[] lastUpdated = (data != null) ? data.lastUpdated() : null;
			String[] propertyNames = propertySource.getPropertyNames();
			if (lastUpdated != null && Arrays.equals(lastUpdated, propertyNames)) {
				return;
			}
			int size = propertyNames.length;
			Map<ConfigurationPropertyName, Set<String>> mappings = cloneOrCreate(
					(data != null) ? data.mappings() : null, size);
			Map<String, ConfigurationPropertyName> reverseMappings = cloneOrCreate(
					(data != null) ? data.reverseMappings() : null, size);
			Set<ConfigurationPropertyName> descendants = (!this.captureDescendants) ? null : new HashSet<>();
			Map<String, Object> systemEnvironmentCopy = (!this.systemEnvironmentSource) ? null
					: copySource(propertySource);
			for (PropertyMapper propertyMapper : this.mappers) {
				for (String propertyName : propertyNames) {
					if (!reverseMappings.containsKey(propertyName)) {
						ConfigurationPropertyName configurationPropertyName = propertyMapper.map(propertyName);
						if (configurationPropertyName != null && !configurationPropertyName.isEmpty()) {
							add(mappings, configurationPropertyName, propertyName);
							reverseMappings.put(propertyName, configurationPropertyName);
						}
					}
				}
			}
			for (String propertyName : propertyNames) {
				addParents(descendants, reverseMappings.get(propertyName));
			}
			ConfigurationPropertyName[] configurationPropertyNames = this.immutable
					? reverseMappings.values().toArray(new ConfigurationPropertyName[0]) : null;
			lastUpdated = this.immutable ? null : propertyNames;
			this.data = new Data(mappings, reverseMappings, descendants, configurationPropertyNames,
					systemEnvironmentCopy, lastUpdated);
		}

		@SuppressWarnings("unchecked")
		private HashMap<String, Object> copySource(EnumerablePropertySource<?> propertySource) {
			return new HashMap<>((Map<String, Object>) propertySource.getSource());
		}

		private <K, V> Map<K, V> cloneOrCreate(Map<K, V> source, int size) {
			return (source != null) ? new LinkedHashMap<>(source) : new LinkedHashMap<>(size);
		}

		private void addParents(Set<ConfigurationPropertyName> descendants, ConfigurationPropertyName name) {
			if (descendants == null || name == null || name.isEmpty()) {
				return;
			}
			ConfigurationPropertyName parent = name.getParent();
			while (!parent.isEmpty()) {
				if (!descendants.add(parent)) {
					return;
				}
				parent = parent.getParent();
			}
		}

		private <K, T> void add(Map<K, Set<T>> map, K key, T value) {
			map.computeIfAbsent(key, (k) -> new HashSet<>()).add(value);
		}

		Set<String> getMapped(ConfigurationPropertyName configurationPropertyName) {
			return this.data.mappings().getOrDefault(configurationPropertyName, Collections.emptySet());
		}

		ConfigurationPropertyName[] getConfigurationPropertyNames(String[] propertyNames) {
			Data data = this.data;
			ConfigurationPropertyName[] names = data.configurationPropertyNames();
			if (names != null) {
				return names;
			}
			Map<String, ConfigurationPropertyName> reverseMappings = data.reverseMappings();
			if (reverseMappings == null || reverseMappings.isEmpty()) {
				return EMPTY_NAMES_ARRAY;
			}
			names = new ConfigurationPropertyName[propertyNames.length];
			for (int i = 0; i < propertyNames.length; i++) {
				names[i] = reverseMappings.get(propertyNames[i]);
			}
			return names;
		}

		Set<ConfigurationPropertyName> getDescendants() {
			return this.data.descendants();
		}

		Object getSystemEnvironmentProperty(String name) {
			return this.data.systemEnvironmentCopy().get(name);
		}

		private record Data(Map<ConfigurationPropertyName, Set<String>> mappings,
				Map<String, ConfigurationPropertyName> reverseMappings, Set<ConfigurationPropertyName> descendants,
				ConfigurationPropertyName[] configurationPropertyNames, Map<String, Object> systemEnvironmentCopy,
				String[] lastUpdated) {

		}

	}

	/**
	 * ConfigurationPropertyNames iterator backed by an array.
	 */
	private static class ConfigurationPropertyNamesIterator implements Iterator<ConfigurationPropertyName> {

		private final ConfigurationPropertyName[] names;

		private int index = 0;

		ConfigurationPropertyNamesIterator(ConfigurationPropertyName[] names) {
			this.names = names;
		}

		@Override
		public boolean hasNext() {
			skipNulls();
			return this.index < this.names.length;
		}

		@Override
		public ConfigurationPropertyName next() {
			skipNulls();
			if (this.index >= this.names.length) {
				throw new NoSuchElementException();
			}
			return this.names[this.index++];
		}

		private void skipNulls() {
			while (this.index < this.names.length) {
				if (this.names[this.index] != null) {
					return;
				}
				this.index++;
			}
		}

	}

}
