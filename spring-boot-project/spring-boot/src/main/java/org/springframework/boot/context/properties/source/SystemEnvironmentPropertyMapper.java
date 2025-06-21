/*
 * Copyright 2012-present the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.ToStringFormat;

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
	public List<String> map(ConfigurationPropertyName configurationPropertyName) {
		List<String> mapped = new ArrayList<>(4);
		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.SYSTEM_ENVIRONMENT, true));
		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.LEGACY_SYSTEM_ENVIRONMENT, true));
		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.SYSTEM_ENVIRONMENT, false));
		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.LEGACY_SYSTEM_ENVIRONMENT, false));
		return mapped;
	}

	private void addIfMissing(List<String> list, String value) {
		if (!list.contains(value)) {
			list.add(value);
		}
	}

	@Override
	public ConfigurationPropertyName map(String propertySourceName) {
		return convertName(propertySourceName);
	}

	private ConfigurationPropertyName convertName(String propertySourceName) {
		try {
			return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
		}
		catch (Exception ex) {
			return ConfigurationPropertyName.EMPTY;
		}
	}

	private CharSequence processElementValue(CharSequence value) {
		String result = value.toString().toLowerCase(Locale.ENGLISH);
		return isNumber(result) ? "[" + result + "]" : result;
	}

	private static boolean isNumber(String string) {
		return string.chars().allMatch(Character::isDigit);
	}

	@Override
	public BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
		return this::isAncestorOf;
	}

	private boolean isAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
		return name.isAncestorOf(candidate) || isLegacyAncestorOf(name, candidate);
	}

	private boolean isLegacyAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
		if (!name.hasDashedElement()) {
			return false;
		}
		ConfigurationPropertyName legacyCompatibleName = name.asSystemEnvironmentLegacyName();
		return legacyCompatibleName != null && legacyCompatibleName.isAncestorOf(candidate);
	}

}
