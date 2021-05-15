/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Test {@link PropertyMapper} implementation.
 */
class TestPropertyMapper implements PropertyMapper {

	private MultiValueMap<ConfigurationPropertyName, String> fromConfig = new LinkedMultiValueMap<>();

	private Map<String, ConfigurationPropertyName> fromSource = new LinkedHashMap<>();

	void addFromPropertySource(String from, String to) {
		this.fromSource.put(from, ConfigurationPropertyName.of(to));
	}

	void addFromConfigurationProperty(ConfigurationPropertyName from, String... to) {
		for (String propertySourceName : to) {
			this.fromConfig.add(from, propertySourceName);
		}
	}

	@Override
	public List<String> map(ConfigurationPropertyName configurationPropertyName) {
		return this.fromConfig.getOrDefault(configurationPropertyName, Collections.emptyList());
	}

	@Override
	public ConfigurationPropertyName map(String propertySourceName) {
		return this.fromSource.getOrDefault(propertySourceName, ConfigurationPropertyName.EMPTY);
	}

}
