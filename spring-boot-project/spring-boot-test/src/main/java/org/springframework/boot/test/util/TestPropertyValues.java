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

package org.springframework.boot.test.util;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
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

	private static final TestPropertyValues EMPTY = new TestPropertyValues(
			Collections.emptyMap());

	private final Map<String, Object> properties;

	private TestPropertyValues(Map<String, Object> properties) {
		this.properties = Collections.unmodifiableMap(properties);
	}

	/**
	 * Builder method to add more properties.
	 * @param pairs the property pairs to add
	 * @return a new {@link TestPropertyValues} instance
	 */
	public TestPropertyValues and(String... pairs) {
		return and(Arrays.stream(pairs).map(Pair::parse));
	}

	private TestPropertyValues and(Stream<Pair> pairs) {
		Map<String, Object> properties = new LinkedHashMap<>(this.properties);
		pairs.filter(Objects::nonNull).forEach((pair) -> pair.addTo(properties));
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
	 * duration of the {@code call}, restoring previous values when the call completes.
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

	@SuppressWarnings("unchecked")
	private <E extends Throwable> void rethrow(Throwable e) throws E {
		throw (E) e;
	}

	@SuppressWarnings("unchecked")
	private void addToSources(MutablePropertySources sources, Type type, String name) {
		if (sources.contains(name)) {
			PropertySource<?> propertySource = sources.get(name);
			if (propertySource.getClass().equals(type.getSourceClass())) {
				((Map<String, Object>) propertySource.getSource())
						.putAll(this.properties);
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
		if (pairs == null) {
			return empty();
		}
		return of(StreamSupport.stream(pairs.spliterator(), false));
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
		if (pairs == null) {
			return empty();
		}
		return empty().and(pairs.map(Pair::parse));
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

		Type(Class<? extends MapPropertySource> sourceClass, String suffix) {
			this.sourceClass = sourceClass;
			this.suffix = (suffix == null ? null : "-" + suffix);
		}

		public Class<? extends MapPropertySource> getSourceClass() {
			return this.sourceClass;
		}

		protected String applySuffix(String name) {
			return (this.suffix == null ? name : name + "-" + this.suffix);
		}

	}

	/**
	 * A single name value pair.
	 */
	public static class Pair {

		private String name;

		private String value;

		public Pair(String name, String value) {
			Assert.hasLength(name, "Name must not be empty");
			this.name = name;
			this.value = value;
		}

		public void addTo(Map<String, Object> properties) {
			properties.put(this.name, this.value);
		}

		public static Pair parse(String pair) {
			int index = getSeparatorIndex(pair);
			String name = (index > 0 ? pair.substring(0, index) : pair);
			String value = (index > 0 ? pair.substring(index + 1) : "");
			return of(name.trim(), value.trim());
		}

		private static int getSeparatorIndex(String pair) {
			int colonIndex = pair.indexOf(":");
			int equalIndex = pair.indexOf("=");
			if (colonIndex == -1) {
				return equalIndex;
			}
			if (equalIndex == -1) {
				return colonIndex;
			}
			return Math.min(colonIndex, equalIndex);
		}

		private static Pair of(String name, String value) {
			if (StringUtils.isEmpty(name) && StringUtils.isEmpty(value)) {
				return null;
			}
			return new Pair(name, value);
		}

	}

	/**
	 * Handler to apply and restore system properties.
	 */
	private class SystemPropertiesHandler implements Closeable {

		private final Map<String, String> previous;

		SystemPropertiesHandler() {
			this.previous = apply(TestPropertyValues.this.properties);
		}

		private Map<String, String> apply(Map<String, ?> properties) {
			Map<String, String> previous = new LinkedHashMap<>();
			properties.forEach((name, value) -> previous.put(name,
					setOrClear(name, (String) value)));
			return previous;
		}

		@Override
		public void close() {
			this.previous.forEach(this::setOrClear);
		}

		private String setOrClear(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			if (StringUtils.isEmpty(value)) {
				return (String) System.getProperties().remove(name);
			}
			return (String) System.getProperties().setProperty(name, value);
		}

	}

}
