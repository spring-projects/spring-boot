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

	/**
	 * Constructs a new SpringIterableConfigurationPropertySource with the given property
	 * source and mappers.
	 * @param propertySource the underlying EnumerablePropertySource to be wrapped
	 * @param mappers the PropertyMapper instances to be used for mapping properties
	 * @throws IllegalArgumentException if the propertySource is not an
	 * EnumerablePropertySource
	 */
	SpringIterableConfigurationPropertySource(EnumerablePropertySource<?> propertySource, PropertyMapper... mappers) {
		super(propertySource, mappers);
		assertEnumerablePropertySource();
		this.ancestorOfCheck = getAncestorOfCheck(mappers);
		this.cache = new SoftReferenceConfigurationPropertyCache<>(isImmutablePropertySource());
	}

	/**
	 * Returns a {@code BiPredicate} that checks if a given
	 * {@code ConfigurationPropertyName} is an ancestor of another
	 * {@code ConfigurationPropertyName} based on the provided {@code PropertyMapper}
	 * array.
	 * @param mappers the array of {@code PropertyMapper} objects used to determine the
	 * ancestor relationship
	 * @return a {@code BiPredicate} that checks if a given
	 * {@code ConfigurationPropertyName} is an ancestor of another
	 * {@code ConfigurationPropertyName}
	 */
	private BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck(
			PropertyMapper[] mappers) {
		BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> ancestorOfCheck = mappers[0]
			.getAncestorOfCheck();
		for (int i = 1; i < mappers.length; i++) {
			ancestorOfCheck = ancestorOfCheck.or(mappers[i].getAncestorOfCheck());
		}
		return ancestorOfCheck;
	}

	/**
	 * Asserts that the property source is fully enumerable.
	 * @throws IllegalArgumentException if the property source is not fully enumerable
	 */
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

	/**
	 * Returns the caching configuration property.
	 * @return the caching configuration property
	 */
	@Override
	public ConfigurationPropertyCaching getCaching() {
		return this.cache;
	}

	/**
	 * Retrieves the configuration property with the given name from this configuration
	 * property source.
	 * @param name the name of the configuration property to retrieve
	 * @return the configuration property with the given name, or null if not found
	 */
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
				return ConfigurationProperty.of(this, name, value, origin);
			}
		}
		return null;
	}

	/**
	 * Returns a stream of ConfigurationPropertyName objects.
	 * @return a stream of ConfigurationPropertyName objects
	 */
	@Override
	public Stream<ConfigurationPropertyName> stream() {
		ConfigurationPropertyName[] names = getConfigurationPropertyNames();
		return Arrays.stream(names).filter(Objects::nonNull);
	}

	/**
	 * Returns an iterator over the configuration property names in this configuration
	 * property source.
	 * @return an iterator over the configuration property names
	 */
	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return new ConfigurationPropertyNamesIterator(getConfigurationPropertyNames());
	}

	/**
	 * Determines if the configuration property source contains a descendant of the
	 * specified name.
	 * @param name the name of the configuration property
	 * @return the state of the configuration property (PRESENT, ABSENT, or UNKNOWN)
	 */
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

	/**
	 * Returns an array of ConfigurationPropertyName objects representing the names of the
	 * configuration properties available in this
	 * SpringIterableConfigurationPropertySource.
	 *
	 * If the property source is not immutable, the method retrieves the property names
	 * from the mappings and returns the corresponding ConfigurationPropertyName objects.
	 *
	 * If the property source is immutable, the method checks if the configuration
	 * property names have already been retrieved and stored in the
	 * configurationPropertyNames field. If not, it retrieves the property names from the
	 * mappings and stores the corresponding ConfigurationPropertyName objects in the
	 * configurationPropertyNames field.
	 * @return an array of ConfigurationPropertyName objects representing the names of the
	 * configuration properties available in this
	 * SpringIterableConfigurationPropertySource
	 */
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

	/**
	 * Retrieves the mappings from the cache.
	 * @return The mappings stored in the cache.
	 */
	private Mappings getMappings() {
		return this.cache.get(this::createMappings, this::updateMappings);
	}

	/**
	 * Creates a new instance of {@link Mappings} using the provided mappers, immutable
	 * property source flag, and ancestor of check flag.
	 * @return the created {@link Mappings} instance
	 */
	private Mappings createMappings() {
		return new Mappings(getMappers(), isImmutablePropertySource(),
				this.ancestorOfCheck == PropertyMapper.DEFAULT_ANCESTOR_OF_CHECK);
	}

	/**
	 * Updates the mappings in the given {@link Mappings} object using the property names
	 * from the property source.
	 * @param mappings the {@link Mappings} object to update
	 * @return the updated {@link Mappings} object
	 */
	private Mappings updateMappings(Mappings mappings) {
		mappings.updateMappings(getPropertySource()::getPropertyNames);
		return mappings;
	}

	/**
	 * Checks if the property source is immutable.
	 * @return true if the property source is immutable, false otherwise
	 */
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

	/**
	 * Returns the property source as an EnumerablePropertySource.
	 * @return the property source as an EnumerablePropertySource
	 */
	@Override
	protected EnumerablePropertySource<?> getPropertySource() {
		return (EnumerablePropertySource<?>) super.getPropertySource();
	}

	/**
	 * Mappings class.
	 */
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

		/**
		 * Constructs a new instance of Mappings with the specified property mappers,
		 * immutability flag, and descendant tracking flag.
		 * @param mappers the array of property mappers to be used for mapping properties
		 * @param immutable the flag indicating whether the mappings should be immutable
		 * @param trackDescendants the flag indicating whether descendant mappings should
		 * be tracked
		 */
		Mappings(PropertyMapper[] mappers, boolean immutable, boolean trackDescendants) {
			this.mappers = mappers;
			this.immutable = immutable;
			this.trackDescendants = trackDescendants;
		}

		/**
		 * Updates the mappings of the Mappings class using the provided property names.
		 * @param propertyNames a Supplier that provides an array of property names
		 * @throws ConcurrentModificationException if the mappings are being modified
		 * concurrently
		 */
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

		/**
		 * Updates the mappings based on the given property names.
		 * @param propertyNames the array of property names to update the mappings with
		 */
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

		/**
		 * Creates a clone of the given source map or creates a new map with the specified
		 * size.
		 * @param <K> the type of keys maintained by the map
		 * @param <V> the type of mapped values
		 * @param source the source map to clone, can be null
		 * @param size the initial size of the new map if source is null
		 * @return a new map that is a clone of the source map if it is not null,
		 * otherwise a new map with the specified size
		 */
		private <K, V> Map<K, V> cloneOrCreate(Map<K, V> source, int size) {
			return (source != null) ? new LinkedHashMap<>(source) : new LinkedHashMap<>(size);
		}

		/**
		 * Adds the parents of a given configuration property name to a map of
		 * descendants.
		 * @param descendants the map of descendants to add the parents to
		 * @param name the configuration property name to add the parents for
		 */
		private void addParents(Map<ConfigurationPropertyName, Set<ConfigurationPropertyName>> descendants,
				ConfigurationPropertyName name) {
			ConfigurationPropertyName parent = name;
			while (!parent.isEmpty()) {
				add(descendants, parent, name);
				parent = parent.getParent();
			}
		}

		/**
		 * Adds a value to the set associated with the specified key in the given map.
		 * @param <K> the type of keys maintained by the map
		 * @param <T> the type of values in the set
		 * @param map the map to add the value to
		 * @param key the key to associate the value with
		 * @param value the value to be added
		 */
		private <K, T> void add(Map<K, Set<T>> map, K key, T value) {
			map.computeIfAbsent(key, (k) -> new HashSet<>()).add(value);
		}

		/**
		 * Retrieves the set of mapped strings associated with the given configuration
		 * property name.
		 * @param configurationPropertyName the configuration property name to retrieve
		 * the mappings for
		 * @return the set of mapped strings for the given configuration property name, or
		 * an empty set if no mappings are found
		 */
		Set<String> getMapped(ConfigurationPropertyName configurationPropertyName) {
			return this.mappings.getOrDefault(configurationPropertyName, Collections.emptySet());
		}

		/**
		 * Retrieves the ConfigurationPropertyName objects corresponding to the given
		 * property names.
		 * @param propertyNames an array of property names
		 * @return an array of ConfigurationPropertyName objects
		 */
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

		/**
		 * Determines the state of a configuration property based on its name and a custom
		 * ancestor check.
		 * @param name The name of the configuration property.
		 * @param ancestorOfCheck The custom ancestor check to determine if the property
		 * is a descendant of another property.
		 * @return The state of the configuration property.
		 */
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

		/**
		 * Constructs a new ConfigurationPropertyNamesIterator with the specified array of
		 * ConfigurationPropertyName objects.
		 * @param names the array of ConfigurationPropertyName objects to iterate over
		 */
		ConfigurationPropertyNamesIterator(ConfigurationPropertyName[] names) {
			this.names = names;
		}

		/**
		 * Returns true if there is at least one more element in the iteration, false
		 * otherwise.
		 * @return true if there is at least one more element in the iteration, false
		 * otherwise
		 */
		@Override
		public boolean hasNext() {
			skipNulls();
			return this.index < this.names.length;
		}

		/**
		 * Returns the next ConfigurationPropertyName in the iteration.
		 * @throws NoSuchElementException if there are no more elements in the iteration
		 * @return the next ConfigurationPropertyName in the iteration
		 */
		@Override
		public ConfigurationPropertyName next() {
			skipNulls();
			if (this.index >= this.names.length) {
				throw new NoSuchElementException();
			}
			return this.names[this.index++];
		}

		/**
		 * Skips over any null values in the names array.
		 * @return void
		 */
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
