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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.PropertySource;

/**
 * Default {@link PropertyMapper} implementation. Names are mapped by removing invalid
 * characters and converting to lower case. For example "{@code my.server_name.PORT}" is
 * mapped to "{@code my.servername.port}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see PropertySourceConfigurationPropertySource
 */
class DefaultPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

	private final Cache<ConfigurationPropertyName> configurationPropertySourceCache = new Cache<>();

	private final Cache<String> propertySourceCache = new Cache<>();

	private final ConfigurationPropertyNameBuilder nameBuilder = new ConfigurationPropertyNameBuilder();

	@Override
	public List<PropertyMapping> map(PropertySource<?> propertySource,
			ConfigurationPropertyName configurationPropertyName) {
		List<PropertyMapping> mapping = this.configurationPropertySourceCache
				.get(configurationPropertyName);
		if (mapping == null) {
			String convertedName = configurationPropertyName.toString();
			mapping = Collections.singletonList(
					new PropertyMapping(convertedName, configurationPropertyName));
		}
		return mapping;
	}

	@Override
	public List<PropertyMapping> map(PropertySource<?> propertySource,
			String propertySourceName) {
		return this.propertySourceCache.computeIfAbsent(propertySourceName, this::tryMap);
	}

	private List<PropertyMapping> tryMap(String propertySourceName) {
		try {
			ConfigurationPropertyName convertedName = this.nameBuilder
					.from(propertySourceName, '.');
			PropertyMapping o = new PropertyMapping(propertySourceName, convertedName);
			return Collections.singletonList(o);
		}
		catch (Exception ex) {
			return Collections.emptyList();
		}
	}

	private static class Cache<K> extends LinkedHashMap<K, List<PropertyMapping>> {

		private final int capacity;

		Cache() {
			this(1);
		}

		Cache(int capacity) {
			super(capacity, (float) 0.75, true);
			this.capacity = capacity;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, List<PropertyMapping>> eldest) {
			return size() >= this.capacity;

		}

	}

}
