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

package org.springframework.boot.actuate.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link Endpoint @Endpoint} to expose {@link ConfigurableEnvironment environment}
 * information.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@Endpoint(id = "env")
public class EnvironmentEndpoint {

	private final Sanitizer sanitizer;

	private final Environment environment;

	public EnvironmentEndpoint(Environment environment) {
		this(environment, Collections.emptyList());
	}

	public EnvironmentEndpoint(Environment environment, Iterable<SanitizingFunction> sanitizingFunctions) {
		this.environment = environment;
		this.sanitizer = new Sanitizer(sanitizingFunctions);
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	public void keysToSanitize(String... keysToSanitize) {
		this.sanitizer.keysToSanitize(keysToSanitize);
	}

	@ReadOperation
	public EnvironmentDescriptor environment(@Nullable String pattern) {
		if (StringUtils.hasText(pattern)) {
			return getEnvironmentDescriptor(Pattern.compile(pattern).asPredicate());
		}
		return getEnvironmentDescriptor((name) -> true);
	}

	@ReadOperation
	public EnvironmentEntryDescriptor environmentEntry(@Selector String toMatch) {
		return getEnvironmentEntryDescriptor(toMatch);
	}

	private EnvironmentDescriptor getEnvironmentDescriptor(Predicate<String> propertyNamePredicate) {
		PlaceholdersResolver resolver = getResolver();
		List<PropertySourceDescriptor> propertySources = new ArrayList<>();
		getPropertySourcesAsMap().forEach((sourceName, source) -> {
			if (source instanceof EnumerablePropertySource) {
				propertySources.add(describeSource(sourceName, (EnumerablePropertySource<?>) source, resolver,
						propertyNamePredicate));
			}
		});
		return new EnvironmentDescriptor(Arrays.asList(this.environment.getActiveProfiles()), propertySources);
	}

	private EnvironmentEntryDescriptor getEnvironmentEntryDescriptor(String propertyName) {
		Map<String, PropertyValueDescriptor> descriptors = getPropertySourceDescriptors(propertyName);
		PropertySummaryDescriptor summary = getPropertySummaryDescriptor(descriptors);
		return new EnvironmentEntryDescriptor(summary, Arrays.asList(this.environment.getActiveProfiles()),
				toPropertySourceDescriptors(descriptors));
	}

	private List<PropertySourceEntryDescriptor> toPropertySourceDescriptors(
			Map<String, PropertyValueDescriptor> descriptors) {
		List<PropertySourceEntryDescriptor> result = new ArrayList<>();
		descriptors.forEach((name, property) -> result.add(new PropertySourceEntryDescriptor(name, property)));
		return result;
	}

	private PropertySummaryDescriptor getPropertySummaryDescriptor(Map<String, PropertyValueDescriptor> descriptors) {
		for (Map.Entry<String, PropertyValueDescriptor> entry : descriptors.entrySet()) {
			if (entry.getValue() != null) {
				return new PropertySummaryDescriptor(entry.getKey(), entry.getValue().getValue());
			}
		}
		return null;
	}

	private Map<String, PropertyValueDescriptor> getPropertySourceDescriptors(String propertyName) {
		Map<String, PropertyValueDescriptor> propertySources = new LinkedHashMap<>();
		PlaceholdersResolver resolver = getResolver();
		getPropertySourcesAsMap().forEach((sourceName, source) -> propertySources.put(sourceName,
				source.containsProperty(propertyName) ? describeValueOf(propertyName, source, resolver) : null));
		return propertySources;
	}

	private PropertySourceDescriptor describeSource(String sourceName, EnumerablePropertySource<?> source,
			PlaceholdersResolver resolver, Predicate<String> namePredicate) {
		Map<String, PropertyValueDescriptor> properties = new LinkedHashMap<>();
		Stream.of(source.getPropertyNames()).filter(namePredicate)
				.forEach((name) -> properties.put(name, describeValueOf(name, source, resolver)));
		return new PropertySourceDescriptor(sourceName, properties);
	}

	@SuppressWarnings("unchecked")
	private PropertyValueDescriptor describeValueOf(String name, PropertySource<?> source,
			PlaceholdersResolver resolver) {
		Object resolved = resolver.resolvePlaceholders(source.getProperty(name));
		Origin origin = ((source instanceof OriginLookup) ? ((OriginLookup<Object>) source).getOrigin(name) : null);
		Object sanitizedValue = sanitize(source, name, resolved);
		return new PropertyValueDescriptor(stringifyIfNecessary(sanitizedValue), origin);
	}

	private PlaceholdersResolver getResolver() {
		return new PropertySourcesPlaceholdersSanitizingResolver(getPropertySources(), this.sanitizer);
	}

	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
		for (PropertySource<?> source : getPropertySources()) {
			if (!ConfigurationPropertySources.isAttachedConfigurationPropertySource(source)) {
				extract("", map, source);
			}
		}
		return map;
	}

	private MutablePropertySources getPropertySources() {
		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
			return configurableEnvironment.getPropertySources();
		}
		return new StandardEnvironment().getPropertySources();
	}

	private void extract(String root, Map<String, PropertySource<?>> map, PropertySource<?> source) {
		if (source instanceof CompositePropertySource compositePropertySource) {
			for (PropertySource<?> nest : compositePropertySource.getPropertySources()) {
				extract(source.getName() + ":", map, nest);
			}
		}
		else {
			map.put(root + source.getName(), source);
		}
	}

	private Object sanitize(PropertySource<?> source, String name, Object value) {
		return this.sanitizer.sanitize(new SanitizableData(source, name, value));
	}

	protected Object stringifyIfNecessary(Object value) {
		if (value == null || ClassUtils.isPrimitiveOrWrapper(value.getClass())
				|| Number.class.isAssignableFrom(value.getClass())) {
			return value;
		}
		if (CharSequence.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}
		return "Complex property type " + value.getClass().getName();
	}

	/**
	 * {@link PropertySourcesPlaceholdersResolver} that sanitizes sensitive placeholders
	 * if present.
	 */
	private static class PropertySourcesPlaceholdersSanitizingResolver extends PropertySourcesPlaceholdersResolver {

		private final Sanitizer sanitizer;

		private final Iterable<PropertySource<?>> sources;

		PropertySourcesPlaceholdersSanitizingResolver(Iterable<PropertySource<?>> sources, Sanitizer sanitizer) {
			super(sources, new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true));
			this.sources = sources;
			this.sanitizer = sanitizer;
		}

		@Override
		protected String resolvePlaceholder(String placeholder) {
			if (this.sources != null) {
				for (PropertySource<?> source : this.sources) {
					Object value = source.getProperty(placeholder);
					if (value != null) {
						SanitizableData data = new SanitizableData(source, placeholder, value);
						Object sanitized = this.sanitizer.sanitize(data);
						return (sanitized != null) ? String.valueOf(sanitized) : null;
					}
				}
			}
			return null;
		}

	}

	/**
	 * A description of an {@link Environment}.
	 */
	public static final class EnvironmentDescriptor {

		private final List<String> activeProfiles;

		private final List<PropertySourceDescriptor> propertySources;

		private EnvironmentDescriptor(List<String> activeProfiles, List<PropertySourceDescriptor> propertySources) {
			this.activeProfiles = activeProfiles;
			this.propertySources = propertySources;
		}

		public List<String> getActiveProfiles() {
			return this.activeProfiles;
		}

		public List<PropertySourceDescriptor> getPropertySources() {
			return this.propertySources;
		}

	}

	/**
	 * A description of an entry of the {@link Environment}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class EnvironmentEntryDescriptor {

		private final PropertySummaryDescriptor property;

		private final List<String> activeProfiles;

		private final List<PropertySourceEntryDescriptor> propertySources;

		private EnvironmentEntryDescriptor(PropertySummaryDescriptor property, List<String> activeProfiles,
				List<PropertySourceEntryDescriptor> propertySources) {
			this.property = property;
			this.activeProfiles = activeProfiles;
			this.propertySources = propertySources;
		}

		public PropertySummaryDescriptor getProperty() {
			return this.property;
		}

		public List<String> getActiveProfiles() {
			return this.activeProfiles;
		}

		public List<PropertySourceEntryDescriptor> getPropertySources() {
			return this.propertySources;
		}

	}

	/**
	 * A summary of a particular entry of the {@link Environment}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertySummaryDescriptor {

		private final String source;

		private final Object value;

		public PropertySummaryDescriptor(String source, Object value) {
			this.source = source;
			this.value = value;
		}

		public String getSource() {
			return this.source;
		}

		public Object getValue() {
			return this.value;
		}

	}

	/**
	 * A description of a {@link PropertySource}.
	 */
	public static final class PropertySourceDescriptor {

		private final String name;

		private final Map<String, PropertyValueDescriptor> properties;

		private PropertySourceDescriptor(String name, Map<String, PropertyValueDescriptor> properties) {
			this.name = name;
			this.properties = properties;
		}

		public String getName() {
			return this.name;
		}

		public Map<String, PropertyValueDescriptor> getProperties() {
			return this.properties;
		}

	}

	/**
	 * A description of a particular entry of {@link PropertySource}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertySourceEntryDescriptor {

		private final String name;

		private final PropertyValueDescriptor property;

		private PropertySourceEntryDescriptor(String name, PropertyValueDescriptor property) {
			this.name = name;
			this.property = property;
		}

		public String getName() {
			return this.name;
		}

		public PropertyValueDescriptor getProperty() {
			return this.property;
		}

	}

	/**
	 * A description of a property's value, including its origin if available.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertyValueDescriptor {

		private final Object value;

		private final String origin;

		private final String[] originParents;

		private PropertyValueDescriptor(Object value, Origin origin) {
			this.value = value;
			this.origin = (origin != null) ? origin.toString() : null;
			List<Origin> originParents = Origin.parentsFrom(origin);
			this.originParents = originParents.isEmpty() ? null
					: originParents.stream().map(Object::toString).toArray(String[]::new);
		}

		public Object getValue() {
			return this.value;
		}

		public String getOrigin() {
			return this.origin;
		}

		public String[] getOriginParents() {
			return this.originParents;
		}

	}

}
