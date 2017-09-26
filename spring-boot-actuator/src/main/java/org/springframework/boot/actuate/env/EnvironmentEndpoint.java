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

package org.springframework.boot.actuate.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor.PropertyValueDescriptor;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Endpoint} to expose {@link ConfigurableEnvironment environment} information.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Endpoint(id = "env")
public class EnvironmentEndpoint {

	private final Sanitizer sanitizer = new Sanitizer();

	private final Environment environment;

	public EnvironmentEndpoint(Environment environment) {
		this.environment = environment;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	@ReadOperation
	public EnvironmentDescriptor environment(String pattern) {
		if (StringUtils.hasText(pattern)) {
			return getEnvironmentDescriptor(Pattern.compile(pattern).asPredicate());
		}
		return getEnvironmentDescriptor((name) -> true);
	}

	@ReadOperation
	public EnvironmentDescriptor environmentEntry(@Selector String toMatch) {
		return getEnvironmentDescriptor(toMatch::equals);
	}

	private EnvironmentDescriptor getEnvironmentDescriptor(
			Predicate<String> propertyNamePredicate) {
		PlaceholdersResolver resolver = getResolver();
		List<PropertySourceDescriptor> propertySources = new ArrayList<>();
		getPropertySourcesAsMap().forEach((sourceName, source) -> {
			if (source instanceof EnumerablePropertySource) {
				propertySources.add(
						describeSource(sourceName, (EnumerablePropertySource<?>) source,
								resolver, propertyNamePredicate));
			}
		});
		return new EnvironmentDescriptor(
				Arrays.asList(this.environment.getActiveProfiles()), propertySources);
	}

	private PropertySourceDescriptor describeSource(String sourceName,
			EnumerablePropertySource<?> source, PlaceholdersResolver resolver,
			Predicate<String> namePredicate) {
		Map<String, PropertyValueDescriptor> properties = new LinkedHashMap<>();
		Stream.of(source.getPropertyNames()).filter(namePredicate).forEach(
				(name) -> properties.put(name, describeValueOf(name, source, resolver)));
		return new PropertySourceDescriptor(sourceName, properties);
	}

	private PropertyValueDescriptor describeValueOf(String name,
			EnumerablePropertySource<?> source, PlaceholdersResolver resolver) {
		Object resolved = resolver.resolvePlaceholders(source.getProperty(name));
		@SuppressWarnings("unchecked")
		String origin = (source instanceof OriginLookup)
				? ((OriginLookup<Object>) source).getOrigin(name).toString() : null;
		return new PropertyValueDescriptor(sanitize(name, resolved), origin);
	}

	private PlaceholdersResolver getResolver() {
		return new PropertySourcesPlaceholdersSanitizingResolver(
				getPropertySources(), this.sanitizer);
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
		if (this.environment instanceof ConfigurableEnvironment) {
			sources = ((ConfigurableEnvironment) this.environment).getPropertySources();
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
	 * {@link PropertySourcesPlaceholdersResolver} that sanitizes sensitive placeholders
	 * if present.
	 */
	private static class PropertySourcesPlaceholdersSanitizingResolver
			extends PropertySourcesPlaceholdersResolver {

		private final Sanitizer sanitizer;

		PropertySourcesPlaceholdersSanitizingResolver(
				Iterable<PropertySource<?>> sources, Sanitizer sanitizer) {
			super(sources, new PropertyPlaceholderHelper(
					SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX,
					SystemPropertyUtils.VALUE_SEPARATOR, true));
			this.sanitizer = sanitizer;
		}

		@Override
		protected String resolvePlaceholder(String placeholder) {
			String value = super.resolvePlaceholder(placeholder);
			return (value != null ?
					(String) this.sanitizer.sanitize(placeholder, value) : null);
		}

	}

	/**
	 * A description of an {@link Environment}.
	 */
	public static final class EnvironmentDescriptor {

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
		public static final class PropertySourceDescriptor {

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
			public static final class PropertyValueDescriptor {

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

	/**
	 * Exception thrown when the specified property cannot be found.
	 */
	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such property")
	public static class NoSuchPropertyException extends RuntimeException {

		public NoSuchPropertyException(String string) {
			super(string);
		}

	}

}
