/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

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

	private final SoftReferenceConfigurationPropertyCache<Mappings> cache;

	private volatile ConfigurationPropertyName[] configurationPropertyNames;

	SpringIterableConfigurationPropertySource(EnumerablePropertySource<?> propertySource, PropertyMapper... mappers) {
		super(propertySource, mappers);
		assertEnumerablePropertySource();
		this.ancestorOfCheck = getAncestorOfCheck(mappers);
		this.cache = new SoftReferenceConfigurationPropertyCache<>(isImmutablePropertySource());
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
		if (getPropertySource() instanceof MapPropertySource) {
			try {
				((MapPropertySource) getPropertySource()).getSource().size();
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
		for (String candidate : getMappings().getMapped(name)) {
			Object value = getPropertySource().getProperty(candidate);
			if (value != null) {
				Origin origin = PropertySourceOrigin.get(getPropertySource(), candidate);
				return ConfigurationProperty.of(name, value, origin);
			}
		}
		return null;
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
			return getMappings().containsDescendantOf(name, this.ancestorOfCheck);
		}
		ConfigurationPropertyName[] candidates = getConfigurationPropertyNames();
		for (ConfigurationPropertyName candidate : candidates) {
			if (candidate != null && this.ancestorOfCheck.test(name, candidate)) {
				return ConfigurationPropertyState.PRESENT;
			}
		}
		return ConfigurationPropertyState.ABSENT;
	}

	private ConfigurationPropertyName[] getConfigurationPropertyNames() {
		if (!isImmutablePropertySource()) {
			return getMappings().getConfigurationPropertyNames(getPropertySource().getPropertyNames());
		}
		ConfigurationPropertyName[] configurationPropertyNames = this.configurationPropertyNames;
		if (configurationPropertyNames == null) {
			configurationPropertyNames = getMappings()
					.getConfigurationPropertyNames(getPropertySource().getPropertyNames());
			this.configurationPropertyNames = configurationPropertyNames;
		}
		return configurationPropertyNames;
	}

	private Mappings getMappings() {
		return this.cache.get(this::createMappings, this::updateMappings);
	}

	private Mappings createMappings() {
		return new Mappings(getMappers(), isImmutablePropertySource(),
				this.ancestorOfCheck == PropertyMapper.DEFAULT_ANCESTOR_OF_CHECK);
	}

	private Mappings updateMappings(Mappings mappings) {
		mappings.updateMappings(getPropertySource()::getPropertyNames);
		return mappings;
	}

	private boolean isImmutablePropertySource() {
		EnumerablePropertySource<?> source = getPropertySource();
		if (source instanceof OriginLookup) {
			return ((OriginLookup<?>) source).isImmutable();
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

	private static class Mappings {

		private static final ConfigurationPropertyName[] EMPTY_NAMES_ARRAY = {};

		private final PropertyMapper[] mappers;

		private final boolean immutable;

		private final boolean trackDescendants;

		private volatile Map<ConfigurationPropertyName, Set<String>> mappings;

		private volatile Map<String, ConfigurationPropertyName> reverseMappings;

		private volatile Map<ConfigurationPropertyName, Set<ConfigurationPropertyName>> descendants;

		private volatile ConfigurationPropertyName[] configurationPropertyNames;

		private volatile String[] lastUpdated;

		Mappings(PropertyMapper[] mappers, boolean immutable, boolean trackDescendants) {
			this.mappers = mappers;
			this.immutable = immutable;
			this.trackDescendants = trackDescendants;
		}

		void updateMappings(Supplier<String[]> propertyNames) {
			if (this.mappings == null || !this.immutable) {
				int count = 0;
				while (true) {
					try {
						updateMappings(propertyNames.get());
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

		private void updateMappings(String[] propertyNames) {
			String[] lastUpdated = this.lastUpdated;
			if (lastUpdated != null && Arrays.equals(lastUpdated, propertyNames)) {
				return;
			}
			int size = propertyNames.length;
			Map<ConfigurationPropertyName, Set<String>> mappings = cloneOrCreate(this.mappings, size);
			Map<String, ConfigurationPropertyName> reverseMappings = cloneOrCreate(this.reverseMappings, size);
			Map<ConfigurationPropertyName, Set<ConfigurationPropertyName>> descendants = cloneOrCreate(this.descendants,
					size);
			for (PropertyMapper propertyMapper : this.mappers) {
				for (String propertyName : propertyNames) {
					if (!reverseMappings.containsKey(propertyName)) {
						ConfigurationPropertyName configurationPropertyName = propertyMapper.map(propertyName);
						if (configurationPropertyName != null && !configurationPropertyName.isEmpty()) {
							add(mappings, configurationPropertyName, propertyName);
							reverseMappings.put(propertyName, configurationPropertyName);
							if (this.trackDescendants) {
								addParents(descendants, configurationPropertyName);
							}
						}
					}
				}
			}
			this.mappings = mappings;
			this.reverseMappings = reverseMappings;
			this.descendants = descendants;
			this.lastUpdated = this.immutable ? null : propertyNames;
			this.configurationPropertyNames = this.immutable
					? reverseMappings.values().toArray(new ConfigurationPropertyName[0]) : null;
		}

		private <K, V> Map<K, V> cloneOrCreate(Map<K, V> source, int size) {
			return (source != null) ? new LinkedHashMap<>(source) : new LinkedHashMap<>(size);
		}

		private void addParents(Map<ConfigurationPropertyName, Set<ConfigurationPropertyName>> descendants,
				ConfigurationPropertyName name) {
			ConfigurationPropertyName parent = name;
			while (!parent.isEmpty()) {
				add(descendants, parent, name);
				parent = parent.getParent();
			}
		}

		private <K, T> void add(Map<K, Set<T>> map, K key, T value) {
			map.computeIfAbsent(key, (k) -> new HashSet<>()).add(value);
		}

		Set<String> getMapped(ConfigurationPropertyName configurationPropertyName) {
			return this.mappings.getOrDefault(configurationPropertyName, Collections.emptySet());
		}

		ConfigurationPropertyName[] getConfigurationPropertyNames(String[] propertyNames) {
			ConfigurationPropertyName[] names = this.configurationPropertyNames;
			if (names != null) {
				return names;
			}
			Map<String, ConfigurationPropertyName> reverseMappings = this.reverseMappings;
			if (reverseMappings == null || reverseMappings.isEmpty()) {
				return EMPTY_NAMES_ARRAY;
			}
			names = new ConfigurationPropertyName[propertyNames.length];
			for (int i = 0; i < propertyNames.length; i++) {
				names[i] = reverseMappings.get(propertyNames[i]);
			}
			return names;
		}

		ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name,
				BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> ancestorOfCheck) {
			if (name.isEmpty() && !this.descendants.isEmpty()) {
				return ConfigurationPropertyState.PRESENT;
			}
			Set<ConfigurationPropertyName> candidates = this.descendants.getOrDefault(name, Collections.emptySet());
			for (ConfigurationPropertyName candidate : candidates) {
				if (ancestorOfCheck.test(name, candidate)) {
					return ConfigurationPropertyState.PRESENT;
				}
			}
			return ConfigurationPropertyState.ABSENT;
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
