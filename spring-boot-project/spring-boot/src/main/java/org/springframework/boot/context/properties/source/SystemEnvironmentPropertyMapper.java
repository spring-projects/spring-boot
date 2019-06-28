/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.Locale;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

/**
 * {@link PropertyMapper} for system environment variables. Names are mapped by removing
 * invalid characters, converting to lower case and replacing "{@code _}" with
 * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
 * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
 * "{@code host[0]}").
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class SystemEnvironmentPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

	@Override
	public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
		String name = convertName(configurationPropertyName);
		String legacyName = convertLegacyName(configurationPropertyName);
		if (name.equals(legacyName)) {
			return new PropertyMapping[] { new PropertyMapping(name, configurationPropertyName) };
		}
		return new PropertyMapping[] { new PropertyMapping(name, configurationPropertyName),
				new PropertyMapping(legacyName, configurationPropertyName) };
	}

	@Override
	public PropertyMapping[] map(String propertySourceName) {
		ConfigurationPropertyName name = convertName(propertySourceName);
		if (name == null || name.isEmpty()) {
			return NO_MAPPINGS;
		}
		return new PropertyMapping[] { new PropertyMapping(propertySourceName, name) };
	}

	private ConfigurationPropertyName convertName(String propertySourceName) {
		try {
			return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private String convertName(ConfigurationPropertyName name) {
		return convertName(name, name.getNumberOfElements());
	}

	private String convertName(ConfigurationPropertyName name, int numberOfElements) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < numberOfElements; i++) {
			if (result.length() > 0) {
				result.append("_");
			}
			result.append(name.getElement(i, Form.UNIFORM).toUpperCase(Locale.ENGLISH));
		}
		return result.toString();
	}

	private String convertLegacyName(ConfigurationPropertyName name) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < name.getNumberOfElements(); i++) {
			if (result.length() > 0) {
				result.append("_");
			}
			result.append(convertLegacyNameElement(name.getElement(i, Form.ORIGINAL)));
		}
		return result.toString();
	}

	private Object convertLegacyNameElement(String element) {
		return element.replace('-', '_').toUpperCase(Locale.ENGLISH);
	}

	private CharSequence processElementValue(CharSequence value) {
		String result = value.toString().toLowerCase(Locale.ENGLISH);
		return isNumber(result) ? "[" + result + "]" : result;
	}

	private static boolean isNumber(String string) {
		return string.chars().allMatch(Character::isDigit);
	}

}
