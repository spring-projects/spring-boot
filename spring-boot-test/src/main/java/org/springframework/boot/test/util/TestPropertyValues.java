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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.Assert;

/**
 * Test utilities for adding properties to the environment. The type of {@link PropertySource}
 * to be added can be specified by {@link Type}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public final class TestPropertyValues {

	private final Map<String, Object> properties = new HashMap<>();

	private TestPropertyValues(String[] pairs) {
		addProperties(pairs);
	}

	/**
	 * Return a new {@link TestPropertyValues} with the underlying map populated with the given property pairs.
	 * Name-value pairs can be specified with colon (":") or equals ("=") separators.
	 * @param pairs The key value pairs for properties that need to be added to the environment
	 * @return the new instance
	 */
	public static TestPropertyValues of(String... pairs) {
		return new TestPropertyValues(pairs);
	}

	/**
	 * Builder method to append another property to the underlying map of properties.
	 * @param key The property key
	 * @param value The property value
	 * @return the existing instance of {@link TestPropertyValues}
	 */
	public TestPropertyValues and(String key, String value) {
		this.properties.put(key, value);
		return this;
	}

	private void addProperties(String[] pairs) {
		for (String pair : pairs) {
			int index = getSeparatorIndex(pair);
			String key = pair.substring(0, index > 0 ? index : pair.length());
			String value = index > 0 ? pair.substring(index + 1) : "";
			this.properties.put(key.trim(), value.trim());
		}
	}

	/**
	 * Add the properties from the underlying map to the environment owned by an {@link ApplicationContext}.
	 * @param context the context with an environment to modify
	 */
	public void applyTo(ConfigurableApplicationContext context) {
		applyTo(context.getEnvironment());
	}

	/**
	 * Add the properties from the underlying map to the environment. The default property source used is
	 * {@link MapPropertySource}.
	 * @param environment the environment that needs to be modified
	 */
	public void applyTo(ConfigurableEnvironment environment) {
		applyTo(environment, Type.MAP);
	}

	/**
	 * Add the properties from the underlying map to the environment using the specified property source type.
	 * @param environment the environment that needs to be modified
	 * @param type the type of {@link PropertySource} to be added. See {@link Type}
	 */
	public void applyTo(ConfigurableEnvironment environment, Type type) {
		applyTo(environment, type, "test");
	}

	/**
	 * Add the properties from the underlying map to the environment using the specified property source type and name.
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

	@SuppressWarnings("unchecked")
	private void addToSources(MutablePropertySources sources, Type type, String name) {
		if (sources.contains(name)) {
			PropertySource<?> propertySource = sources.get(name);
			if (propertySource.getClass().equals(type.getSourceClass())) {
				Map<String, Object> source = (Map<String, Object>) propertySource.getSource();
				source.putAll(this.properties);
				return;
			}
		}
		MapPropertySource source = (type.equals(Type.MAP) ? new MapPropertySource(name, this.properties) :
				new SystemEnvironmentPropertySource(name, this.properties));
		sources.addFirst(source);
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

}

