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

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.Assert;

/**
 * A managed set of {@link ConfigurationPropertySource} instances, usually adapted from
 * Spring's {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see #attach(MutablePropertySources)
 * @see #get(PropertySources)
 */
public class ConfigurationPropertySources
		implements Iterable<ConfigurationPropertySource> {

	/**
	 * The name of the {@link PropertySource} {@link #adapt adapter}.
	 */
	public static final String PROPERTY_SOURCE_NAME = "configurationProperties";

	private final PropertySources propertySources;

	private final Map<PropertySource<?>, ConfigurationPropertySource> adapters = new WeakHashMap<>();

	/**
	 * Create a new {@link ConfigurationPropertySources} instance.
	 * @param propertySources the property sources to expose
	 */
	ConfigurationPropertySources(PropertySources propertySources) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		this.propertySources = propertySources;
	}

	@Override
	public Iterator<ConfigurationPropertySource> iterator() {
		return streamPropertySources(this.propertySources)
				.filter(s -> !(s instanceof ConfigurationPropertySourcesPropertySource))
				.map(this::adapt).collect(Collectors.toList()).iterator();
	}

	private Stream<PropertySource<?>> streamPropertySources(PropertySources sources) {
		return StreamSupport.stream(sources.spliterator(), false).flatMap(this::flatten);
	}

	private Stream<PropertySource<?>> flatten(PropertySource<?> source) {
		if (source.getSource() instanceof ConfigurableEnvironment) {
			return streamPropertySources(
					((ConfigurableEnvironment) source.getSource()).getPropertySources());
		}
		return Stream.of(source);
	}

	private ConfigurationPropertySource adapt(PropertySource<?> source) {
		return this.adapters.computeIfAbsent(source, (k) -> {
			return new PropertySourceConfigurationPropertySource(source,
					getPropertyMapper(source));
		});
	}

	private PropertyMapper getPropertyMapper(PropertySource<?> source) {
		if (source instanceof SystemEnvironmentPropertySource) {
			return SystemEnvironmentPropertyMapper.INSTANCE;
		}
		return DefaultPropertyMapper.INSTANCE;
	}

	/**
	 * Attach a {@link ConfigurationPropertySources} instance to the specified
	 * {@link ConfigurableEnvironment} so that classic
	 * {@link PropertySourcesPropertyResolver} calls will resolve using
	 * {@link ConfigurationPropertyName configuration property names}.
	 * @param environment the source environment
	 * @return the instance attached
	 */
	public static ConfigurationPropertySources attach(
			ConfigurableEnvironment environment) {
		return attach(environment.getPropertySources());
	}

	/**
	 * Attach a {@link ConfigurationPropertySources} instance to the specified
	 * {@link PropertySources} so that classic {@link PropertySourcesPropertyResolver}
	 * calls will resolve using using {@link ConfigurationPropertyName configuration
	 * property names}.
	 * @param propertySources the source property sources
	 * @return the instance attached
	 */
	public static ConfigurationPropertySources attach(
			MutablePropertySources propertySources) {
		ConfigurationPropertySources adapted = new ConfigurationPropertySources(
				propertySources);
		propertySources.addFirst(new ConfigurationPropertySourcesPropertySource(
				PROPERTY_SOURCE_NAME, adapted));
		return adapted;
	}

	/**
	 * Get a {@link ConfigurationPropertySources} instance for the specified
	 * {@link PropertySources} (either previously {@link #attach(MutablePropertySources)
	 * attached} or a new instance.
	 * @param propertySources the source property sources
	 * @return a {@link ConfigurationPropertySources} instance
	 */
	public static ConfigurationPropertySources get(PropertySources propertySources) {
		if (propertySources == null) {
			return null;
		}
		PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
		if (source != null) {
			return (ConfigurationPropertySources) source.getSource();
		}
		return new ConfigurationPropertySources(propertySources);
	}

	/**
	 * Get a {@link ConfigurationPropertySources} instance for the {@link PropertySources}
	 * from the specified {@link ConfigurableEnvironment}, (either previously
	 * {@link #attach(MutablePropertySources) attached} or a new instance.
	 * @param environment the configurable environment
	 * @return a {@link ConfigurationPropertySources} instance
	 */
	public static ConfigurationPropertySources get(ConfigurableEnvironment environment) {
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
		if (source != null) {
			return (ConfigurationPropertySources) source.getSource();
		}
		return new ConfigurationPropertySources(propertySources);
	}

}
