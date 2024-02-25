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

package org.springframework.boot.actuate.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.Show;
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
import org.springframework.util.StringUtils;

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

	private final Show showValues;

	/**
	 * Constructs a new EnvironmentEndpoint with the specified environment, sanitizing
	 * functions, and show values.
	 * @param environment the environment to be used by the endpoint
	 * @param sanitizingFunctions the sanitizing functions to be used by the endpoint
	 * @param showValues the show values to be used by the endpoint
	 */
	public EnvironmentEndpoint(Environment environment, Iterable<SanitizingFunction> sanitizingFunctions,
			Show showValues) {
		this.environment = environment;
		this.sanitizer = new Sanitizer(sanitizingFunctions);
		this.showValues = showValues;
	}

	/**
	 * Retrieves the environment descriptor based on the specified pattern.
	 * @param pattern the pattern to filter the environment properties (nullable)
	 * @return the environment descriptor
	 */
	@ReadOperation
	public EnvironmentDescriptor environment(@Nullable String pattern) {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getEnvironmentDescriptor(pattern, showUnsanitized);
	}

	/**
	 * Retrieves the environment descriptor based on the given pattern and showUnsanitized
	 * flag.
	 * @param pattern the pattern to match against environment names
	 * @param showUnsanitized flag indicating whether to include unsanitized environment
	 * names
	 * @return the environment descriptor matching the given pattern and showUnsanitized
	 * flag
	 */
	EnvironmentDescriptor getEnvironmentDescriptor(String pattern, boolean showUnsanitized) {
		if (StringUtils.hasText(pattern)) {
			return getEnvironmentDescriptor(Pattern.compile(pattern).asPredicate(), showUnsanitized);
		}
		return getEnvironmentDescriptor((name) -> true, showUnsanitized);
	}

	/**
	 * Retrieves the environment descriptor based on the given property name predicate and
	 * showUnsanitized flag.
	 * @param propertyNamePredicate the predicate used to filter property names
	 * @param showUnsanitized the flag indicating whether to show unsanitized properties
	 * @return the environment descriptor
	 */
	private EnvironmentDescriptor getEnvironmentDescriptor(Predicate<String> propertyNamePredicate,
			boolean showUnsanitized) {
		List<PropertySourceDescriptor> propertySources = new ArrayList<>();
		getPropertySourcesAsMap().forEach((sourceName, source) -> {
			if (source instanceof EnumerablePropertySource) {
				propertySources.add(describeSource(sourceName, (EnumerablePropertySource<?>) source,
						propertyNamePredicate, showUnsanitized));
			}
		});
		return new EnvironmentDescriptor(Arrays.asList(this.environment.getActiveProfiles()),
				Arrays.asList(this.environment.getDefaultProfiles()), propertySources);
	}

	/**
	 * Retrieves the environment entry descriptor based on the provided selector.
	 * @param toMatch the selector used to match the environment entry
	 * @return the environment entry descriptor matching the selector
	 */
	@ReadOperation
	public EnvironmentEntryDescriptor environmentEntry(@Selector String toMatch) {
		boolean showUnsanitized = this.showValues.isShown(true);
		return getEnvironmentEntryDescriptor(toMatch, showUnsanitized);
	}

	/**
	 * Retrieves the descriptor for the environment entry with the specified property
	 * name.
	 * @param propertyName the name of the property
	 * @param showUnsanitized flag indicating whether to show unsanitized values
	 * @return the descriptor for the environment entry
	 */
	EnvironmentEntryDescriptor getEnvironmentEntryDescriptor(String propertyName, boolean showUnsanitized) {
		Map<String, PropertyValueDescriptor> descriptors = getPropertySourceDescriptors(propertyName, showUnsanitized);
		PropertySummaryDescriptor summary = getPropertySummaryDescriptor(descriptors);
		return new EnvironmentEntryDescriptor(summary, Arrays.asList(this.environment.getActiveProfiles()),
				Arrays.asList(this.environment.getDefaultProfiles()), toPropertySourceDescriptors(descriptors));
	}

	/**
	 * Converts a map of property value descriptors to a list of property source entry
	 * descriptors.
	 * @param descriptors the map of property value descriptors
	 * @return the list of property source entry descriptors
	 */
	private List<PropertySourceEntryDescriptor> toPropertySourceDescriptors(
			Map<String, PropertyValueDescriptor> descriptors) {
		List<PropertySourceEntryDescriptor> result = new ArrayList<>();
		descriptors.forEach((name, property) -> result.add(new PropertySourceEntryDescriptor(name, property)));
		return result;
	}

	/**
	 * Retrieves the PropertySummaryDescriptor based on the provided descriptors.
	 * @param descriptors a map containing the descriptors for the properties
	 * @return the PropertySummaryDescriptor if a non-null value is found, otherwise null
	 */
	private PropertySummaryDescriptor getPropertySummaryDescriptor(Map<String, PropertyValueDescriptor> descriptors) {
		for (Map.Entry<String, PropertyValueDescriptor> entry : descriptors.entrySet()) {
			if (entry.getValue() != null) {
				return new PropertySummaryDescriptor(entry.getKey(), entry.getValue().getValue());
			}
		}
		return null;
	}

	/**
	 * Retrieves the descriptors of property sources for a given property name.
	 * @param propertyName the name of the property to retrieve descriptors for
	 * @param showUnsanitized a flag indicating whether to show unsanitized values
	 * @return a map of property source descriptors, where the key is the source name and
	 * the value is the property value descriptor
	 */
	private Map<String, PropertyValueDescriptor> getPropertySourceDescriptors(String propertyName,
			boolean showUnsanitized) {
		Map<String, PropertyValueDescriptor> propertySources = new LinkedHashMap<>();
		getPropertySourcesAsMap().forEach((sourceName, source) -> propertySources.put(sourceName,
				source.containsProperty(propertyName) ? describeValueOf(propertyName, source, showUnsanitized) : null));
		return propertySources;
	}

	/**
	 * Describes the given property source.
	 * @param sourceName the name of the property source
	 * @param source the property source to describe
	 * @param namePredicate the predicate to filter property names
	 * @param showUnsanitized flag indicating whether to show unsanitized values
	 * @return the descriptor of the property source
	 */
	private PropertySourceDescriptor describeSource(String sourceName, EnumerablePropertySource<?> source,
			Predicate<String> namePredicate, boolean showUnsanitized) {
		Map<String, PropertyValueDescriptor> properties = new LinkedHashMap<>();
		Stream.of(source.getPropertyNames())
			.filter(namePredicate)
			.forEach((name) -> properties.put(name, describeValueOf(name, source, showUnsanitized)));
		return new PropertySourceDescriptor(sourceName, properties);
	}

	/**
	 * Describes the value of a property in a given property source.
	 * @param name the name of the property
	 * @param source the property source
	 * @param showUnsanitized flag indicating whether to show unsanitized value
	 * @return the property value descriptor
	 */
	@SuppressWarnings("unchecked")
	private PropertyValueDescriptor describeValueOf(String name, PropertySource<?> source, boolean showUnsanitized) {
		PlaceholdersResolver resolver = new PropertySourcesPlaceholdersResolver(getPropertySources());
		Object resolved = resolver.resolvePlaceholders(source.getProperty(name));
		Origin origin = ((source instanceof OriginLookup) ? ((OriginLookup<Object>) source).getOrigin(name) : null);
		Object sanitizedValue = sanitize(source, name, resolved, showUnsanitized);
		return new PropertyValueDescriptor(stringifyIfNecessary(sanitizedValue), origin);
	}

	/**
	 * Returns the property sources as a map.
	 * @return a map containing the property sources
	 */
	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
		for (PropertySource<?> source : getPropertySources()) {
			if (!ConfigurationPropertySources.isAttachedConfigurationPropertySource(source)) {
				extract("", map, source);
			}
		}
		return map;
	}

	/**
	 * Returns the property sources of the environment.
	 * @return the property sources of the environment
	 */
	private MutablePropertySources getPropertySources() {
		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
			return configurableEnvironment.getPropertySources();
		}
		return new StandardEnvironment().getPropertySources();
	}

	/**
	 * Recursively extracts property sources from a given root property source and adds
	 * them to a map.
	 * @param root The root name of the property source.
	 * @param map The map to store the extracted property sources.
	 * @param source The property source to extract from.
	 */
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

	/**
	 * Sanitizes the given property value based on the provided property source, name, and
	 * showUnsanitized flag.
	 * @param source the property source from which the value is retrieved
	 * @param name the name of the property
	 * @param value the value of the property
	 * @param showUnsanitized flag indicating whether to show unsanitized values
	 * @return the sanitized property value
	 */
	private Object sanitize(PropertySource<?> source, String name, Object value, boolean showUnsanitized) {
		return this.sanitizer.sanitize(new SanitizableData(source, name, value), showUnsanitized);
	}

	/**
	 * Converts the given value to a string if necessary.
	 * @param value the value to be converted
	 * @return the converted value as a string, or the original value if it is null, a
	 * primitive or wrapper type, or a number
	 * @throws NullPointerException if the value is null
	 */
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
	 * Description of an {@link Environment}.
	 */
	public static final class EnvironmentDescriptor implements OperationResponseBody {

		private final List<String> activeProfiles;

		private final List<String> defaultProfiles;

		private final List<PropertySourceDescriptor> propertySources;

		/**
		 * Constructs a new EnvironmentDescriptor with the specified active profiles,
		 * default profiles, and property sources.
		 * @param activeProfiles the list of active profiles
		 * @param defaultProfiles the list of default profiles
		 * @param propertySources the list of property sources
		 */
		private EnvironmentDescriptor(List<String> activeProfiles, List<String> defaultProfiles,
				List<PropertySourceDescriptor> propertySources) {
			this.activeProfiles = activeProfiles;
			this.defaultProfiles = defaultProfiles;
			this.propertySources = propertySources;
		}

		/**
		 * Returns the list of active profiles.
		 * @return the list of active profiles
		 */
		public List<String> getActiveProfiles() {
			return this.activeProfiles;
		}

		/**
		 * Returns the list of default profiles.
		 * @return the list of default profiles
		 */
		public List<String> getDefaultProfiles() {
			return this.defaultProfiles;
		}

		/**
		 * Returns the list of property sources in this environment descriptor.
		 * @return the list of property sources
		 */
		public List<PropertySourceDescriptor> getPropertySources() {
			return this.propertySources;
		}

	}

	/**
	 * Description of an entry of the {@link Environment}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class EnvironmentEntryDescriptor {

		private final PropertySummaryDescriptor property;

		private final List<String> activeProfiles;

		private final List<String> defaultProfiles;

		private final List<PropertySourceEntryDescriptor> propertySources;

		/**
		 * Constructs a new EnvironmentEntryDescriptor with the specified property, active
		 * profiles, default profiles, and property sources.
		 * @param property the property summary descriptor for this environment entry
		 * @param activeProfiles the list of active profiles for this environment entry
		 * @param defaultProfiles the list of default profiles for this environment entry
		 * @param propertySources the list of property source entry descriptors for this
		 * environment entry
		 */
		EnvironmentEntryDescriptor(PropertySummaryDescriptor property, List<String> activeProfiles,
				List<String> defaultProfiles, List<PropertySourceEntryDescriptor> propertySources) {
			this.property = property;
			this.activeProfiles = activeProfiles;
			this.defaultProfiles = defaultProfiles;
			this.propertySources = propertySources;
		}

		/**
		 * Returns the PropertySummaryDescriptor object associated with this
		 * EnvironmentEntryDescriptor.
		 * @return the PropertySummaryDescriptor object associated with this
		 * EnvironmentEntryDescriptor
		 */
		public PropertySummaryDescriptor getProperty() {
			return this.property;
		}

		/**
		 * Returns the list of active profiles.
		 * @return the list of active profiles
		 */
		public List<String> getActiveProfiles() {
			return this.activeProfiles;
		}

		/**
		 * Returns the list of default profiles.
		 * @return the list of default profiles
		 */
		public List<String> getDefaultProfiles() {
			return this.defaultProfiles;
		}

		/**
		 * Returns the list of property sources.
		 * @return the list of property sources
		 */
		public List<PropertySourceEntryDescriptor> getPropertySources() {
			return this.propertySources;
		}

	}

	/**
	 * Description of a particular entry of the {@link Environment}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertySummaryDescriptor {

		private final String source;

		private final Object value;

		/**
		 * Constructs a new PropertySummaryDescriptor with the specified source and value.
		 * @param source the source of the property summary
		 * @param value the value of the property summary
		 */
		public PropertySummaryDescriptor(String source, Object value) {
			this.source = source;
			this.value = value;
		}

		/**
		 * Returns the source of the PropertySummaryDescriptor.
		 * @return the source of the PropertySummaryDescriptor
		 */
		public String getSource() {
			return this.source;
		}

		/**
		 * Returns the value of the property.
		 * @return the value of the property
		 */
		public Object getValue() {
			return this.value;
		}

	}

	/**
	 * Description of a {@link PropertySource}.
	 */
	public static final class PropertySourceDescriptor {

		private final String name;

		private final Map<String, PropertyValueDescriptor> properties;

		/**
		 * Constructs a new PropertySourceDescriptor with the specified name and
		 * properties.
		 * @param name the name of the property source
		 * @param properties the map of properties associated with the property source
		 */
		private PropertySourceDescriptor(String name, Map<String, PropertyValueDescriptor> properties) {
			this.name = name;
			this.properties = properties;
		}

		/**
		 * Returns the name of the PropertySourceDescriptor.
		 * @return the name of the PropertySourceDescriptor
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the map of properties associated with this PropertySourceDescriptor.
		 * @return the map of properties, where the key is a string representing the
		 * property name and the value is a PropertyValueDescriptor object representing
		 * the property value and its descriptor
		 */
		public Map<String, PropertyValueDescriptor> getProperties() {
			return this.properties;
		}

	}

	/**
	 * Description of a particular entry of {@link PropertySource}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertySourceEntryDescriptor {

		private final String name;

		private final PropertyValueDescriptor property;

		/**
		 * Constructs a new PropertySourceEntryDescriptor with the specified name and
		 * property.
		 * @param name the name of the property source entry
		 * @param property the property value descriptor
		 */
		private PropertySourceEntryDescriptor(String name, PropertyValueDescriptor property) {
			this.name = name;
			this.property = property;
		}

		/**
		 * Returns the name of the PropertySourceEntryDescriptor.
		 * @return the name of the PropertySourceEntryDescriptor
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the property value descriptor of this PropertySourceEntryDescriptor.
		 * @return the property value descriptor
		 */
		public PropertyValueDescriptor getProperty() {
			return this.property;
		}

	}

	/**
	 * Description of a property's value, including its origin if available.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static final class PropertyValueDescriptor {

		private final Object value;

		private final String origin;

		private final String[] originParents;

		/**
		 * Constructs a new PropertyValueDescriptor with the specified value and origin.
		 * @param value the value of the property
		 * @param origin the origin of the property
		 */
		private PropertyValueDescriptor(Object value, Origin origin) {
			this.value = value;
			this.origin = (origin != null) ? origin.toString() : null;
			List<Origin> originParents = Origin.parentsFrom(origin);
			this.originParents = originParents.isEmpty() ? null
					: originParents.stream().map(Object::toString).toArray(String[]::new);
		}

		/**
		 * Returns the value of the property.
		 * @return the value of the property
		 */
		public Object getValue() {
			return this.value;
		}

		/**
		 * Returns the origin of the property value.
		 * @return the origin of the property value
		 */
		public String getOrigin() {
			return this.origin;
		}

		/**
		 * Returns an array of origin parents.
		 * @return an array of origin parents
		 */
		public String[] getOriginParents() {
			return this.originParents;
		}

	}

}
