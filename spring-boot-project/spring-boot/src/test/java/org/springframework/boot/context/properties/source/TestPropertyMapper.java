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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Test {@link PropertyMapper} implementation.
 */
class TestPropertyMapper implements PropertyMapper {

	private MultiValueMap<String, PropertyMapping> fromSource = new LinkedMultiValueMap<>();

	private MultiValueMap<ConfigurationPropertyName, PropertyMapping> fromConfig = new LinkedMultiValueMap<>();

	public void addFromPropertySource(String from, String... to) {
		for (String configurationPropertyName : to) {
			this.fromSource.add(from, new PropertyMapping(from,
					ConfigurationPropertyName.of(configurationPropertyName)));
		}
	}

	public void addFromConfigurationProperty(ConfigurationPropertyName from,
			String... to) {
		for (String propertySourceName : to) {
			this.fromConfig.add(from, new PropertyMapping(propertySourceName, from));
		}
	}

	@Override
	public PropertyMapping[] map(String propertySourceName) {
		return this.fromSource.getOrDefault(propertySourceName, Collections.emptyList())
				.toArray(new PropertyMapping[0]);
	}

	@Override
	public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
		return this.fromConfig
				.getOrDefault(configurationPropertyName, Collections.emptyList())
				.toArray(new PropertyMapping[0]);
	}

}
