/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.util;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Test utilities for adding properties. Properties can be applied to a Spring
 * {@link Environment} or to the {@link System#getProperties() system environment}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class TestPropertyValues {

	private static final TestPropertyValues EMPTY = new TestPropertyValues(Collections.emptyMap());

	private final Map<String, Object> properties;

	/**
	 * Constructs a new TestPropertyValues object with the given properties.
	 * @param properties the map of properties to be set
	 */
	private TestPropertyValues(Map<String, Object> properties) {
		this.properties = Collections.unmodifiableMap(properties);
	}

	/**
	 * Return a new {@link TestPropertyValues} instance with additional entries.
	 * Name-value pairs can be specified with colon (":") or equals ("=") separators.
	 * @param pairs the property pairs to add
	 * @return a new {@link TestPropertyValues} instance
	 */
	public TestPropertyValues and(String... pairs) {
		return and(Arrays.stream(pairs), Pair::parse);
	}

	/**
	 * Return a new {@link TestPropertyValues} instance with additional entries.
	 * Name-value pairs can be specified with colon (":") or equals ("=") separators.
	 * @param pairs the property pairs to add
	 * @return a new {@link TestPropertyValues} instance
	 * @since 2.4.0
	 */
	public TestPropertyValues and(Iterable<String> pairs) {
		return (pairs != null) ? and(StreamSupport.stream(pairs.spliterator(), false)) : this;
	}

	/**
	 * Return a new {@link TestPropertyValues} instance with additional entries.
	 * Name-value pairs can be specified with colon (":") or equals ("=") separators.
	 * @param pairs the property pairs to add
	 * @return a new {@link TestPropertyValues} instance
	 * @since 2.4.0
	 */
	public TestPropertyValues and(Stream<String> pairs) {
		return (pairs != null) ? and(pairs, Pair::parse) : this;
	}

	/**
	 * Return a new {@link TestPropertyValues} instance with additional entries.
	 * @param map the map of properties that need to be added to the environment
	 * @return a new {@link TestPropertyValues} instance
	 * @since 2.4.0
	 */
	public TestPropertyValues and(Map<String, String> map) {
		return (map != null) ? and(map.entrySet().stream(), Pair::fromMapEntry) : this;
	}

	/**
	 * Return a new {@link TestPropertyValues} instance with additional entries.
	 * @param <T> the stream element type
	 * @param stream the elements that need to be added to the environment
	 * @param mapper a mapper function to convert an element from the stream into a
	 * {@link Pair}
	 * @return a new {@link TestPropertyValues} instance
	 * @since 2.4.0
	 */
	public <T> TestPropertyValues and(Stream<T> stream, Function<T, Pair> mapper) {
		if (stream == null) {
			return this;
		}
		Map<String, Object> properties = new LinkedHashMap<>(this.properties);
		stream.map(mapper).filter(Objects::nonNull).forEach((pair) -> pair.addTo(properties));
		return new TestPropertyValues(properties);
	}

	/**
	 * Add the properties from the underlying map to the environment owned by an
	 * {@link ApplicationContext}.
	 * @param context the context with an environment to modify
	 */
	public void applyTo(ConfigurableApplicationContext context) {
		applyTo(context.getEnvironment());
	}

	/**
	 * Add the properties from the underlying map to the environment. The default property
	 * source used is {@link MapPropertySource}.
	 * @param environment the environment that needs to be modified
	 */
	public void applyTo(ConfigurableEnvironment environment) {
		applyTo(environment, Type.MAP);
	}

	/**
	 * Add the properties from the underlying map to the environment using the specified
	 * property source type.
	 * @param environment the environment that needs to be modified
	 * @param type the type of {@link PropertySource} to be added. See {@link Type}
	 */
	public void applyTo(ConfigurableEnvironment environment, Type type) {
		applyTo(environment, type, type.applySuffix("test"));
	}

	/**
	 * Add the properties from the underlying map to the environment using the specified
	 * property source type and name.
	 * @param environment the environment that needs to be modified
	 * @param type the type of {@link PropertySource} to be added. See {@link Type}
	 * @param name the name for the property source
	 */
	public void applyTo(ConfigurableEnvironment environment, Type type, String name) {
		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(type, "Property source type must not be null");
		Assert.notNull(name, "Property source name must not be null");
		MutablePropertySources sources = environment.getPropertySources();
		addToSources(sources, type, name);
		ConfigurationPropertySources.attach(environment);
	}

	/**
	 * Add the properties to the {@link System#getProperties() system properties} for the
	 * duration of the {@code action}, restoring previous values when it completes.
	 * @param action the action to take
	 * @since 3.0.0
	 */
	public void applyToSystemProperties(Runnable action) {
		applyToSystemProperties(() -> {
			action.run();
			return null;
		});
	}

	/**
	 * Add the properties to the {@link System#getProperties() system properties} for the
	 * duration of the {@code call}, restoring previous values when it completes.
	 * @param <T> the result type
	 * @param call the call to make
	 * @return the result of the call
	 */
	public <T> T applyToSystemProperties(Callable<T> call) {
		try (SystemPropertiesHandler handler = new SystemPropertiesHandler()) {
			return call.call();
		}
		catch (Exception ex) {
			rethrow(ex);
			throw new IllegalStateException("Original cause not rethrown", ex);
		}
	}

	/**
	 * Rethrows the specified throwable.
	 * @param e the throwable to be rethrown
	 * @param <E> the type of the throwable
	 * @throws E the rethrown throwable
	 */
	@SuppressWarnings("unchecked")
	private <E extends Throwable> void rethrow(Throwable e) throws E {
		throw (E) e;
	}

	/**
	 * Adds the given properties to the specified property sources.
	 * @param sources the mutable property sources to add the properties to
	 * @param type the type of property source to add (either Type.MAP or
	 * Type.SYSTEM_ENVIRONMENT)
	 * @param name the name of the property source
	 */
	@SuppressWarnings("unchecked")
	private void addToSources(MutablePropertySources sources, Type type, String name) {
		if (sources.contains(name)) {
			PropertySource<?> propertySource = sources.get(name);
			if (propertySource.getClass() == type.getSourceClass()) {
				((Map<String, Object>) propertySource.getSource()).putAll(this.properties);
				return;
			}
		}
		Map<String, Object> source = new LinkedHashMap<>(this.properties);
		sources.addFirst((type.equals(Type.MAP) ? new MapPropertySource(name, source)
				: new SystemEnvironmentPropertySource(name, source)));
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pairs. Name-value pairs can be specified with colon (":") or equals
	 * ("=") separators.
	 * @param pairs the name-value pairs for properties that need to be added to the
	 * environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(String... pairs) {
		return of(Stream.of(pairs));
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pairs. Name-value pairs can be specified with colon (":") or equals
	 * ("=") separators.
	 * @param pairs the name-value pairs for properties that need to be added to the
	 * environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(Iterable<String> pairs) {
		return (pairs != null) ? of(StreamSupport.stream(pairs.spliterator(), false)) : empty();
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pairs. Name-value pairs can be specified with colon (":") or equals
	 * ("=") separators.
	 * @param pairs the name-value pairs for properties that need to be added to the
	 * environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(Stream<String> pairs) {
		return (pairs != null) ? of(pairs, Pair::parse) : empty();
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given map entries.
	 * @param map the map of properties that need to be added to the environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(Map<String, String> map) {
		return (map != null) ? of(map.entrySet().stream(), Pair::fromMapEntry) : empty();
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given stream.
	 * @param <T> the stream element type
	 * @param stream the elements that need to be added to the environment
	 * @param mapper a mapper function to convert an element from the stream into a
	 * {@link Pair}
	 * @return the new instance
	 */
	public static <T> TestPropertyValues of(Stream<T> stream, Function<T, Pair> mapper) {
		return (stream != null) ? empty().and(stream, mapper) : empty();
	}

	/**
	 * Return an empty {@link TestPropertyValues} instance.
	 * @return an empty instance
	 */
	public static TestPropertyValues empty() {
		return EMPTY;
	}

	/**
	 * The type of property source.
	 */
	public enum Type {

		/**
		 * Used for {@link SystemEnvironmentPropertySource}.
		 */
		SYSTEM_ENVIRONMENT(SystemEnvironmentPropertySource.class,
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME),

		/**
		 * Used for {@link MapPropertySource}.
		 */
		MAP(MapPropertySource.class, null);

		private final Class<? extends MapPropertySource> sourceClass;

		private final String suffix;

		/**
		 * Constructs a new instance of the TestPropertyValues class with the specified
		 * source class and suffix.
		 * @param sourceClass the class of the property source
		 * @param suffix the suffix to be appended to the property source
		 */
		Type(Class<? extends MapPropertySource> sourceClass, String suffix) {
			this.sourceClass = sourceClass;
			this.suffix = suffix;
		}

		/**
		 * Returns the class of the source property for this TestPropertyValues instance.
		 * @return the class of the source property
		 */
		public Class<? extends MapPropertySource> getSourceClass() {
			return this.sourceClass;
		}

		/**
		 * Applies a suffix to the given name.
		 * @param name the name to apply the suffix to
		 * @return the name with the suffix applied
		 */
		protected String applySuffix(String name) {
			return (this.suffix != null) ? name + "-" + this.suffix : name;
		}

	}

	/**
	 * A single name value pair.
	 */
	public static final class Pair {

		private final String name;

		private final String value;

		/**
		 * Constructs a new Pair with the specified name and value.
		 * @param name the name of the Pair (must not be empty)
		 * @param value the value of the Pair
		 * @throws IllegalArgumentException if the name is empty
		 */
		private Pair(String name, String value) {
			Assert.hasLength(name, "Name must not be empty");
			this.name = name;
			this.value = value;
		}

		/**
		 * Adds the name-value pair to the given properties map.
		 * @param properties the map to add the name-value pair to
		 */
		public void addTo(Map<String, Object> properties) {
			properties.put(this.name, this.value);
		}

		/**
		 * Parses a string representation of a pair and returns a Pair object.
		 * @param pair the string representation of the pair
		 * @return a Pair object containing the parsed name and value
		 */
		public static Pair parse(String pair) {
			int index = getSeparatorIndex(pair);
			String name = (index > 0) ? pair.substring(0, index) : pair;
			String value = (index > 0) ? pair.substring(index + 1) : "";
			return of(name.trim(), value.trim());
		}

		/**
		 * Returns the index of the separator character in the given pair string. The
		 * separator character can be either a colon (:) or an equal sign (=). If the pair
		 * string does not contain a colon, the index of the equal sign is returned. If
		 * the pair string does not contain an equal sign, the index of the colon is
		 * returned. If both the colon and equal sign are present, the index of the first
		 * occurring separator character is returned.
		 * @param pair the string representing a pair
		 * @return the index of the separator character in the pair string
		 */
		private static int getSeparatorIndex(String pair) {
			int colonIndex = pair.indexOf(':');
			int equalIndex = pair.indexOf('=');
			if (colonIndex == -1) {
				return equalIndex;
			}
			if (equalIndex == -1) {
				return colonIndex;
			}
			return Math.min(colonIndex, equalIndex);
		}

		/**
		 * Factory method to create a {@link Pair} from a {@code Map.Entry}.
		 * @param entry the map entry
		 * @return the {@link Pair} instance or {@code null}
		 * @since 2.4.0
		 */
		public static Pair fromMapEntry(Map.Entry<String, String> entry) {
			return (entry != null) ? of(entry.getKey(), entry.getValue()) : null;
		}

		/**
		 * Factory method to create a {@link Pair} from a name and value.
		 * @param name the name
		 * @param value the value
		 * @return the {@link Pair} instance or {@code null}
		 * @since 2.4.0
		 */
		public static Pair of(String name, String value) {
			if (StringUtils.hasLength(name) || StringUtils.hasLength(value)) {
				return new Pair(name, value);
			}
			return null;
		}

	}

	/**
	 * Handler to apply and restore system properties.
	 */
	private class SystemPropertiesHandler implements Closeable {

		private final Map<String, String> previous;

		/**
		 * Constructs a new SystemPropertiesHandler object.
		 *
		 * This constructor initializes the previous property values by applying the
		 * properties from the TestPropertyValues object.
		 * @param TestPropertyValues.this.properties The properties to be applied to the
		 * system.
		 */
		SystemPropertiesHandler() {
			this.previous = apply(TestPropertyValues.this.properties);
		}

		/**
		 * Applies the given properties to the system properties and returns a map of the
		 * previous values.
		 * @param properties the properties to be applied
		 * @return a map containing the previous values of the properties
		 */
		private Map<String, String> apply(Map<String, ?> properties) {
			Map<String, String> previous = new LinkedHashMap<>();
			properties.forEach((name, value) -> previous.put(name, setOrClear(name, (String) value)));
			return previous;
		}

		/**
		 * Closes the SystemPropertiesHandler by setting or clearing the previous
		 * properties.
		 *
		 * This method iterates over the previous properties and calls the setOrClear
		 * method for each property.
		 *
		 * @see SystemPropertiesHandler#setOrClear(String)
		 */
		@Override
		public void close() {
			this.previous.forEach(this::setOrClear);
		}

		/**
		 * Sets or clears a system property with the given name and value.
		 * @param name the name of the system property
		 * @param value the value to set for the system property, or null to clear it
		 * @return the previous value of the system property, or null if it didn't exist
		 * @throws IllegalArgumentException if the name is null
		 */
		private String setOrClear(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			if (!StringUtils.hasLength(value)) {
				return (String) System.getProperties().remove(name);
			}
			return (String) System.getProperties().setProperty(name, value);
		}

	}

}
