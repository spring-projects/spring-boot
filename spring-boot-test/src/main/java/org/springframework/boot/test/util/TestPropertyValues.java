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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.Streams;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Test utilities for adding properties. Properties can be applied to a Spring
 * {@link Environment} or to the {@link System#getProperties() system environment}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class TestPropertyValues {

	private final Map<String, Object> properties = new LinkedHashMap<>();

	private TestPropertyValues(String[] pairs) {
		addProperties(pairs);
	}

	private void addProperties(String[] pairs) {
		for (String pair : pairs) {
			and(pair);
		}
	}

	/**
	 * Builder method to append another property pair the underlying map of properties.
	 * @param pair The property pair to add
	 * @return the existing instance of {@link TestPropertyValues}
	 */
	public TestPropertyValues and(String pair) {
		int index = getSeparatorIndex(pair);
		String key = pair.substring(0, index > 0 ? index : pair.length());
		String value = index > 0 ? pair.substring(index + 1) : "";
		and(key.trim(), value.trim());
		return this;
	}

	private int getSeparatorIndex(String pair) {
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

	/**
	 * Builder method to append another property to the underlying map of properties.
	 * @param name The property name
	 * @param value The property value
	 * @return the existing instance of {@link TestPropertyValues}
	 */
	public TestPropertyValues and(String name, String value) {
		if (StringUtils.isEmpty(name) && StringUtils.isEmpty(value)) {
			return this;
		}
		Assert.hasLength(name, "Name must not be empty");
		this.properties.put(name, value);
		return this;
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
		applyTo(environment, type, "test");
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
	 * duration of the {@code call}, restoring previous values then the call completes.
	 * @param <T> the result type
	 * @param call the call to make
	 * @return the result of the call
	 */
	public <T> T applyToSystemProperties(Callable<T> call) {
		try (SystemPropertiesHandler handler = new SystemPropertiesHandler()) {
			return call.call();
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
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
		MapPropertySource source = (type.equals(Type.MAP)
				? new MapPropertySource(name, this.properties)
				: new SystemEnvironmentPropertySource(name, this.properties));
		sources.addFirst(source);
	}

	/**
	 * Return a new empty {@link TestPropertyValues} instance.
	 * @return an empty instance
	 */
	public static TestPropertyValues empty() {
		return of();
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pair.
	 * @param name the property name
	 * @param value the property value
	 * @return the new instance
	 */

	public static TestPropertyValues ofPair(String name, String value) {
		return of().and(name, value);
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pairs. Name-value pairs can be specified with colon (":") or equals
	 * ("=") separators.
	 * @param pairs The key value pairs for properties that need to be added to the
	 * environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(Iterable<String> pairs) {
		if (pairs == null) {
			return of();
		}
		return of(Streams.stream(pairs).toArray(String[]::new));
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the
	 * given property pairs. Name-value pairs can be specified with colon (":") or equals
	 * ("=") separators.
	 * @param pairs The key value pairs for properties that need to be added to the
	 * environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(String... pairs) {
		return new TestPropertyValues(pairs);
	}

	/**
	 * The type of property source.
	 */
	public enum Type {

		/**
		 * Used for {@link SystemEnvironmentPropertySource}.
		 */
		SYSTEM(SystemEnvironmentPropertySource.class),

		/**
		 * Used for {@link MapPropertySource}.
		 */
		MAP(MapPropertySource.class);

		private Class<? extends MapPropertySource> sourceClass;

		Type(Class<? extends MapPropertySource> sourceClass) {
			this.sourceClass = sourceClass;
		}

		public Class<? extends MapPropertySource> getSourceClass() {
			return this.sourceClass;
		}

	}

	/**
	 * Handler to apply and restore system properties.
	 */
	private class SystemPropertiesHandler implements Closeable {

		private final Map<String, Object> properties;

		private final Map<String, String> previous;

		SystemPropertiesHandler() {
			this.properties = new LinkedHashMap<>(TestPropertyValues.this.properties);
			this.previous = apply(this.properties);
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
		};

		private String setOrClear(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			if (value == null) {
				return (String) System.getProperties().remove(name);
			}
			return (String) System.getProperties().setProperty(name, value);
		}

	}

}
