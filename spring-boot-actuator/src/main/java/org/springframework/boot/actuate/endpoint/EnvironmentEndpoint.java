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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor.PropertyValueDescriptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link Endpoint} to expose {@link ConfigurableEnvironment environment} information.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "endpoints.env")
public class EnvironmentEndpoint extends AbstractEndpoint<EnvironmentDescriptor> {

	private final Sanitizer sanitizer = new Sanitizer();

	/**
	 * Create a new {@link EnvironmentEndpoint} instance.
	 */
	public EnvironmentEndpoint() {
		super("env");
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	@Override
	public EnvironmentDescriptor invoke() {
		PropertyResolver resolver = getResolver();
		List<PropertySourceDescriptor> propertySources = new ArrayList<PropertySourceDescriptor>();
		getPropertySourcesAsMap().forEach((sourceName, source) -> {
			if (source instanceof EnumerablePropertySource) {
				propertySources.add(describeSource(sourceName,
						(EnumerablePropertySource<?>) source, resolver));
			}
		});
		return new EnvironmentDescriptor(
				Arrays.asList(getEnvironment().getActiveProfiles()), propertySources);
	}

	private PropertySourceDescriptor describeSource(String sourceName,
			EnumerablePropertySource<?> source, PropertyResolver resolver) {
		Map<String, PropertyValueDescriptor> properties = new LinkedHashMap<>();
		for (String name : source.getPropertyNames()) {
			properties.put(name, describeValueOf(name, source, resolver));
		}
		return new PropertySourceDescriptor(sourceName, properties);
	}

	private PropertyValueDescriptor describeValueOf(String name,
			EnumerablePropertySource<?> source, PropertyResolver resolver) {
		Object resolved = resolver.getProperty(name, Object.class);
		@SuppressWarnings("unchecked")
		String origin = (source instanceof OriginLookup)
				? ((OriginLookup<Object>) source).getOrigin(name).toString() : null;
		return new PropertyValueDescriptor(sanitize(name, resolved), origin);
	}

	public PropertyResolver getResolver() {
		PlaceholderSanitizingPropertyResolver resolver = new PlaceholderSanitizingPropertyResolver(
				getPropertySources(), this.sanitizer);
		resolver.setIgnoreUnresolvableNestedPlaceholders(true);
		return resolver;
	}

	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
		for (PropertySource<?> source : getPropertySources()) {
			extract("", map, source);
		}
		return map;
	}

	private MutablePropertySources getPropertySources() {
		MutablePropertySources sources;
		Environment environment = getEnvironment();
		if (environment != null && environment instanceof ConfigurableEnvironment) {
			sources = ((ConfigurableEnvironment) environment).getPropertySources();
		}
		else {
			sources = new StandardEnvironment().getPropertySources();
		}
		return sources;
	}

	private void extract(String root, Map<String, PropertySource<?>> map,
			PropertySource<?> source) {
		if (source instanceof CompositePropertySource) {
			for (PropertySource<?> nest : ((CompositePropertySource) source)
					.getPropertySources()) {
				extract(source.getName() + ":", map, nest);
			}
		}
		else {
			map.put(root + source.getName(), source);
		}
	}

	public Object sanitize(String name, Object object) {
		return this.sanitizer.sanitize(name, object);
	}

	/**
	 * {@link PropertySourcesPropertyResolver} that sanitizes sensitive placeholders if
	 * present.
	 */
	private class PlaceholderSanitizingPropertyResolver
			extends PropertySourcesPropertyResolver {

		private final Sanitizer sanitizer;

		/**
		 * Create a new resolver against the given property sources.
		 * @param propertySources the set of {@link PropertySource} objects to use
		 * @param sanitizer the sanitizer used to sanitize sensitive values
		 */
		PlaceholderSanitizingPropertyResolver(PropertySources propertySources,
				Sanitizer sanitizer) {
			super(propertySources);
			this.sanitizer = sanitizer;
		}

		@Override
		protected String getPropertyAsRawString(String key) {
			String value = super.getPropertyAsRawString(key);
			return (String) this.sanitizer.sanitize(key, value);
		}

	}

	/**
	 * A description of an {@link Environment}.
	 */
	static final class EnvironmentDescriptor {

		private final List<String> activeProfiles;

		private final List<PropertySourceDescriptor> propertySources;

		private EnvironmentDescriptor(List<String> activeProfiles,
				List<PropertySourceDescriptor> propertySources) {
			this.activeProfiles = activeProfiles;
			this.propertySources = propertySources;
		}

		public List<String> getActiveProfiles() {
			return this.activeProfiles;
		}

		public List<PropertySourceDescriptor> getPropertySources() {
			return this.propertySources;
		}

		/**
		 * A description of a {@link PropertySource}.
		 */
		static final class PropertySourceDescriptor {

			private final String name;

			private final Map<String, PropertyValueDescriptor> properties;

			private PropertySourceDescriptor(String name,
					Map<String, PropertyValueDescriptor> properties) {
				this.name = name;
				this.properties = properties;
			}

			public String getName() {
				return this.name;
			}

			public Map<String, PropertyValueDescriptor> getProperties() {
				return this.properties;
			}

			/**
			 * A description of a property's value, including its origin if available.
			 */
			static final class PropertyValueDescriptor {

				private final Object value;

				private final String origin;

				private PropertyValueDescriptor(Object value, String origin) {
					this.value = value;
					this.origin = origin;
				}

				public Object getValue() {
					return this.value;
				}

				public String getOrigin() {
					return this.origin;
				}

			}

		}
	}

}
