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

import org.springframework.util.ObjectUtils;

/**
 * Default {@link PropertyMapper} implementation. Names are mapped by removing invalid
 * characters and converting to lower case. For example "{@code my.server_name.PORT}" is
 * mapped to "{@code my.servername.port}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class DefaultPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

	private LastMapping<ConfigurationPropertyName> lastMappedConfigurationPropertyName;

	private LastMapping<String> lastMappedPropertyName;

	private DefaultPropertyMapper() {
	}

	@Override
	public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
		// Use a local copy in case another thread changes things
		LastMapping<ConfigurationPropertyName> last = this.lastMappedConfigurationPropertyName;
		if (last != null && last.isFrom(configurationPropertyName)) {
			return last.getMapping();
		}
		String convertedName = configurationPropertyName.toString();
		PropertyMapping[] mapping = { new PropertyMapping(convertedName, configurationPropertyName) };
		this.lastMappedConfigurationPropertyName = new LastMapping<>(configurationPropertyName, mapping);
		return mapping;
	}

	@Override
	public PropertyMapping[] map(String propertySourceName) {
		// Use a local copy in case another thread changes things
		LastMapping<String> last = this.lastMappedPropertyName;
		if (last != null && last.isFrom(propertySourceName)) {
			return last.getMapping();
		}
		PropertyMapping[] mapping = tryMap(propertySourceName);
		this.lastMappedPropertyName = new LastMapping<>(propertySourceName, mapping);
		return mapping;
	}

	private PropertyMapping[] tryMap(String propertySourceName) {
		try {
			ConfigurationPropertyName convertedName = ConfigurationPropertyName.adapt(propertySourceName, '.');
			if (!convertedName.isEmpty()) {
				return new PropertyMapping[] { new PropertyMapping(propertySourceName, convertedName) };
			}
		}
		catch (Exception ex) {
		}
		return NO_MAPPINGS;
	}

	private static class LastMapping<T> {

		private final T from;

		private final PropertyMapping[] mapping;

		LastMapping(T from, PropertyMapping[] mapping) {
			this.from = from;
			this.mapping = mapping;
		}

		public boolean isFrom(T from) {
			return ObjectUtils.nullSafeEquals(from, this.from);
		}

		public PropertyMapping[] getMapping() {
			return this.mapping;
		}

	}

}
