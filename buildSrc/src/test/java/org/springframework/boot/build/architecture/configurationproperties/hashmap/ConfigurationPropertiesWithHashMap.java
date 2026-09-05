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

package org.springframework.boot.build.architecture.configurationproperties.hashmap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link HashMap}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithHashMap {

	private Map<String, String> properties = new HashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
