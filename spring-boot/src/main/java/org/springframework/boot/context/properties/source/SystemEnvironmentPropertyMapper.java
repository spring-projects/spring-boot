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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertyMapper} for system environment variables. Names are mapped by removing
 * invalid characters, converting to lower case and replacing "{@code _}" with
 * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
 * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
 * "{@code host[0]}").
 * <p>
 * List shortcuts (names that end with double underscore) are also supported by this
 * mapper. For example, "{@code MY_LIST__=a,b,c}" is mapped to "{@code my.list[0]=a}",
 * "{@code my.list[1]=b}" ,"{@code my.list[2]=c}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see PropertySourceConfigurationPropertySource
 */
class SystemEnvironmentPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

	private final ConfigurationPropertyNameBuilder nameBuilder = new ConfigurationPropertyNameBuilder(
			this::createElement);

	@Override
	public List<PropertyMapping> map(PropertySource<?> propertySource,
			String propertySourceName) {
		ConfigurationPropertyName name = convertName(propertySourceName);
		if (name == null) {
			return Collections.emptyList();
		}
		if (propertySourceName.endsWith("__")) {
			return expandListShortcut(propertySourceName, name,
					propertySource.getProperty(propertySourceName));
		}
		return Collections.singletonList(new PropertyMapping(propertySourceName, name));
	}

	private ConfigurationPropertyName convertName(String propertySourceName) {
		try {
			return this.nameBuilder.from(propertySourceName, '_');
		}
		catch (Exception ex) {
			return null;
		}
	}

	private List<PropertyMapping> expandListShortcut(String propertySourceName,
			ConfigurationPropertyName rootName, Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		List<PropertyMapping> mappings = new ArrayList<>();
		String[] elements = StringUtils
				.commaDelimitedListToStringArray(String.valueOf(value));
		for (int i = 0; i < elements.length; i++) {
			ConfigurationPropertyName name = ConfigurationPropertyName
					.of(rootName.toString() + "[" + i + "]");
			mappings.add(new PropertyMapping(propertySourceName, name,
					new ElementExtractor(i)));
		}
		return mappings;
	}

	@Override
	public List<PropertyMapping> map(PropertySource<?> propertySource,
			ConfigurationPropertyName configurationPropertyName) {
		String name = convertName(configurationPropertyName);
		List<PropertyMapping> result = Collections
				.singletonList(new PropertyMapping(name, configurationPropertyName));
		if (isListShortcutPossible(configurationPropertyName)) {
			result = new ArrayList<>(result);
			result.addAll(mapListShortcut(propertySource, configurationPropertyName));
		}
		return result;
	}

	private String convertName(ConfigurationPropertyName configurationPropertyName) {
		return configurationPropertyName.stream()
				.map(name -> name.getValue(Form.UNIFORM).toUpperCase())
				.collect(Collectors.joining("_"));
	}

	private boolean isListShortcutPossible(ConfigurationPropertyName name) {
		return (name.getElement().isIndexed()
				&& isNumber(name.getElement().getValue(Form.UNIFORM))
				&& name.getParent() != null);
	}

	private List<PropertyMapping> mapListShortcut(PropertySource<?> propertySource,
			ConfigurationPropertyName configurationPropertyName) {
		String propertyName = convertName(configurationPropertyName.getParent()) + "__";
		if (propertySource.containsProperty(propertyName)) {
			int index = Integer.parseInt(
					configurationPropertyName.getElement().getValue(Form.UNIFORM));
			return Collections.singletonList(new PropertyMapping(propertyName,
					configurationPropertyName, new ElementExtractor(index)));
		}
		return Collections.emptyList();
	}

	private String createElement(String value) {
		value = value.toLowerCase();
		return (isNumber(value) ? "[" + value + "]" : value);
	}

	private static boolean isNumber(String string) {
		IntStream nonDigits = string.chars().filter((c) -> !Character.isDigit(c));
		boolean hasNonDigit = nonDigits.findFirst().isPresent();
		return !hasNonDigit;
	}

	/**
	 * Function used to extract an element from a comma list.
	 */
	private static class ElementExtractor implements Function<Object, Object> {

		private final int index;

		ElementExtractor(int index) {
			this.index = index;
		}

		@Override
		public Object apply(Object value) {
			if (value == null) {
				return null;
			}
			return StringUtils
					.commaDelimitedListToStringArray(value.toString())[this.index];
		}

	}

}
