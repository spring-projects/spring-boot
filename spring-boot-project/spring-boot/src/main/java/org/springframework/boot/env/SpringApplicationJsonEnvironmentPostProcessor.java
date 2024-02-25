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

package org.springframework.boot.env;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * An {@link EnvironmentPostProcessor} that parses JSON from
 * {@code spring.application.json} or equivalently {@code SPRING_APPLICATION_JSON} and
 * adds it as a map property source to the {@link Environment}. The new properties are
 * added with higher priority than the system properties.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @since 1.3.0
 */
public class SpringApplicationJsonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * Name of the {@code spring.application.json} property.
	 */
	public static final String SPRING_APPLICATION_JSON_PROPERTY = "spring.application.json";

	/**
	 * Name of the {@code SPRING_APPLICATION_JSON} environment variable.
	 */
	public static final String SPRING_APPLICATION_JSON_ENVIRONMENT_VARIABLE = "SPRING_APPLICATION_JSON";

	private static final String SERVLET_ENVIRONMENT_CLASS = "org.springframework.web."
			+ "context.support.StandardServletEnvironment";

	private static final Set<String> SERVLET_ENVIRONMENT_PROPERTY_SOURCES = new LinkedHashSet<>(
			Arrays.asList(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME));

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

	private int order = DEFAULT_ORDER;

	/**
	 * Returns the order of this SpringApplicationJsonEnvironmentPostProcessor.
	 * @return the order of this SpringApplicationJsonEnvironmentPostProcessor
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the order of the SpringApplicationJsonEnvironmentPostProcessor.
	 * @param order the order value to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Post-processes the environment by processing JSON property sources.
	 * @param environment the configurable environment
	 * @param application the spring application
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.stream()
			.map(JsonPropertyValue::get)
			.filter(Objects::nonNull)
			.findFirst()
			.ifPresent((v) -> processJson(environment, v));
	}

	/**
	 * Processes the given JSON property value and adds it as a property source to the
	 * configurable environment.
	 * @param environment the configurable environment to add the property source to
	 * @param propertyValue the JSON property value to process
	 */
	private void processJson(ConfigurableEnvironment environment, JsonPropertyValue propertyValue) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> map = parser.parseMap(propertyValue.getJson());
		if (!map.isEmpty()) {
			addJsonPropertySource(environment, new JsonPropertySource(propertyValue, flatten(map)));
		}
	}

	/**
	 * Flatten the map keys using period separator.
	 * @param map the map that should be flattened
	 * @return the flattened map
	 */
	private Map<String, Object> flatten(Map<String, Object> map) {
		Map<String, Object> result = new LinkedHashMap<>();
		flatten(null, result, map);
		return result;
	}

	/**
	 * Flattens a nested map into a flat map with dot-separated keys.
	 * @param prefix the prefix to be added to the keys in the resulting flat map (can be
	 * null)
	 * @param result the resulting flat map
	 * @param map the nested map to be flattened
	 */
	private void flatten(String prefix, Map<String, Object> result, Map<String, Object> map) {
		String namePrefix = (prefix != null) ? prefix + "." : "";
		map.forEach((key, value) -> extract(namePrefix + key, result, value));
	}

	/**
	 * Extracts the values from a nested map or collection and adds them to the result
	 * map.
	 * @param name the name of the value being extracted
	 * @param result the map to add the extracted values to
	 * @param value the value to be extracted
	 */
	@SuppressWarnings("unchecked")
	private void extract(String name, Map<String, Object> result, Object value) {
		if (value instanceof Map) {
			if (CollectionUtils.isEmpty((Map<?, ?>) value)) {
				result.put(name, value);
				return;
			}
			flatten(name, result, (Map<String, Object>) value);
		}
		else if (value instanceof Collection) {
			if (CollectionUtils.isEmpty((Collection<?>) value)) {
				result.put(name, value);
				return;
			}
			int index = 0;
			for (Object object : (Collection<Object>) value) {
				extract(name + "[" + index + "]", result, object);
				index++;
			}
		}
		else {
			result.put(name, value);
		}
	}

	/**
	 * Adds a JSON property source to the given environment.
	 * @param environment the configurable environment to add the property source to
	 * @param source the property source to be added
	 */
	private void addJsonPropertySource(ConfigurableEnvironment environment, PropertySource<?> source) {
		MutablePropertySources sources = environment.getPropertySources();
		String name = findPropertySource(sources);
		if (sources.contains(name)) {
			sources.addBefore(name, source);
		}
		else {
			sources.addFirst(source);
		}
	}

	/**
	 * Finds the property source from the given mutable property sources.
	 * @param sources the mutable property sources to search from
	 * @return the name of the property source found, or the name of the standard
	 * environment system properties property source if not found
	 */
	private String findPropertySource(MutablePropertySources sources) {
		if (ClassUtils.isPresent(SERVLET_ENVIRONMENT_CLASS, null)) {
			PropertySource<?> servletPropertySource = sources.stream()
				.filter((source) -> SERVLET_ENVIRONMENT_PROPERTY_SOURCES.contains(source.getName()))
				.findFirst()
				.orElse(null);
			if (servletPropertySource != null) {
				return servletPropertySource.getName();
			}
		}
		return StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;
	}

	/**
	 * JsonPropertySource class.
	 */
	private static class JsonPropertySource extends MapPropertySource implements OriginLookup<String> {

		private final JsonPropertyValue propertyValue;

		/**
		 * Constructs a new JsonPropertySource with the given JsonPropertyValue and source
		 * map.
		 * @param propertyValue the JsonPropertyValue to be used
		 * @param source the source map containing the properties
		 */
		JsonPropertySource(JsonPropertyValue propertyValue, Map<String, Object> source) {
			super(SPRING_APPLICATION_JSON_PROPERTY, source);
			this.propertyValue = propertyValue;
		}

		/**
		 * Retrieves the origin of the property value associated with the given key.
		 * @param key the key used to retrieve the property value
		 * @return the origin of the property value
		 */
		@Override
		public Origin getOrigin(String key) {
			return this.propertyValue.getOrigin();
		}

	}

	/**
	 * JsonPropertyValue class.
	 */
	private static class JsonPropertyValue {

		private static final String[] CANDIDATES = { SPRING_APPLICATION_JSON_PROPERTY,
				SPRING_APPLICATION_JSON_ENVIRONMENT_VARIABLE };

		private final PropertySource<?> propertySource;

		private final String propertyName;

		private final String json;

		/**
		 * Constructs a new instance of the JsonPropertyValue class with the specified
		 * property source, property name, and JSON string.
		 * @param propertySource the property source from which to retrieve the property
		 * value
		 * @param propertyName the name of the property to retrieve
		 * @param json the JSON string to parse
		 */
		JsonPropertyValue(PropertySource<?> propertySource, String propertyName, String json) {
			this.propertySource = propertySource;
			this.propertyName = propertyName;
			this.json = json;
		}

		/**
		 * Returns the JSON string representation of the property value.
		 * @return the JSON string representation of the property value
		 */
		String getJson() {
			return this.json;
		}

		/**
		 * Returns the origin of the property value.
		 * @return the origin of the property value
		 */
		Origin getOrigin() {
			return PropertySourceOrigin.get(this.propertySource, this.propertyName);
		}

		/**
		 * Retrieves the first non-empty string value from the given property source.
		 * @param propertySource the property source to retrieve the value from
		 * @return the first non-empty string value wrapped in a JsonPropertyValue object,
		 * or null if no such value is found
		 */
		static JsonPropertyValue get(PropertySource<?> propertySource) {
			for (String candidate : CANDIDATES) {
				Object value = propertySource.getProperty(candidate);
				if (value instanceof String string && StringUtils.hasLength(string)) {
					return new JsonPropertyValue(propertySource, candidate, string);
				}
			}
			return null;
		}

	}

}
