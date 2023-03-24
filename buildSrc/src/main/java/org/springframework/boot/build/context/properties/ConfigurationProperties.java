/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration properties read from one or more
 * {@code META-INF/spring-configuration-metadata.json} files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
final class ConfigurationProperties {

	private final Map<String, ConfigurationProperty> byName;

	private ConfigurationProperties(List<ConfigurationProperty> properties) {
		Map<String, ConfigurationProperty> byName = new LinkedHashMap<>();
		for (ConfigurationProperty property : properties) {
			byName.put(property.getName(), property);
		}
		this.byName = Collections.unmodifiableMap(byName);
	}

	ConfigurationProperty get(String propertyName) {
		return this.byName.get(propertyName);
	}

	Stream<ConfigurationProperty> stream() {
		return this.byName.values().stream();
	}

	@SuppressWarnings("unchecked")
	static ConfigurationProperties fromFiles(Iterable<File> files) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			List<ConfigurationProperty> properties = new ArrayList<>();
			for (File file : files) {
				Map<String, Object> json = objectMapper.readValue(file, Map.class);
				for (Map<String, Object> property : (List<Map<String, Object>>) json.get("properties")) {
					properties.add(ConfigurationProperty.fromJsonProperties(property));
				}
			}
			return new ConfigurationProperties(properties);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to load configuration metadata", ex);
		}
	}

}
