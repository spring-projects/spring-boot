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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

/**
 * {@link PropertyMapper} for system environment variables. Names are mapped by removing
 * invalid characters, converting to lower case and replacing "{@code _}" with
 * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
 * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
 * "{@code host[0]}").
 * <p>
 * List shortcuts (names that end with double underscore) are also supported by this
 * mapper. For example, "{@code MY_LIST__=a,b,c}" is mapped to "{@code my.list[0]=a}",
 * "{@code my.list[1]=b}", "{@code my.list[2]=c}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class SystemEnvironmentPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

	private SystemEnvironmentPropertyMapper() {
	}

	@Override
	public List<PropertyMapping> map(
			ConfigurationPropertyName configurationPropertyName) {
		Set<String> names = new LinkedHashSet<>();
		names.add(convertName(configurationPropertyName));
		names.add(convertLegacyName(configurationPropertyName));
		List<PropertyMapping> result = new ArrayList<>();
		names.forEach((name) -> result
				.add(new PropertyMapping(name, configurationPropertyName)));
		return result;
	}

	@Override
	public List<PropertyMapping> map(String propertySourceName) {
		ConfigurationPropertyName name = convertName(propertySourceName);
		if (name == null || name.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.singletonList(new PropertyMapping(propertySourceName, name));
	}

	private ConfigurationPropertyName convertName(String propertySourceName) {
		try {
			return ConfigurationPropertyName.adapt(propertySourceName, '_',
					this::processElementValue);
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
			result.append(result.length() == 0 ? "" : "_");
			result.append(name.getElement(i, Form.UNIFORM).toUpperCase());
		}
		return result.toString();
	}

	private String convertLegacyName(ConfigurationPropertyName name) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < name.getNumberOfElements(); i++) {
			result.append(result.length() == 0 ? "" : "_");
			result.append(convertLegacyNameElement(name.getElement(i, Form.ORIGINAL)));
		}
		return result.toString();
	}

	private Object convertLegacyNameElement(String element) {
		return element.replace("-", "_").toUpperCase();
	}

	private CharSequence processElementValue(CharSequence value) {
		String result = value.toString().toLowerCase();
		return (isNumber(result) ? "[" + result + "]" : result);
	}

	private static boolean isNumber(String string) {
		IntStream nonDigits = string.chars().filter((c) -> !Character.isDigit(c));
		boolean hasNonDigit = nonDigits.findFirst().isPresent();
		return !hasNonDigit;
	}

}
