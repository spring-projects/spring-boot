/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Locale;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.config.YamlClientConfigBuilder;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Adapts {@link HazelcastProperties} to {@link HazelcastConnectionDetails}.
 *
 * @author Dmytro Nosan
 */
class PropertiesHazelcastConnectionDetails implements HazelcastConnectionDetails {

	private final HazelcastProperties properties;

	private final ResourceLoader resourceLoader;

	PropertiesHazelcastConnectionDetails(HazelcastProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public ClientConfig getClientConfig() {
		Resource configLocation = this.properties.resolveConfigLocation();
		ClientConfig config = (configLocation != null) ? loadClientConfig(configLocation) : ClientConfig.load();
		config.setClassLoader(this.resourceLoader.getClassLoader());
		return config;
	}

	private ClientConfig loadClientConfig(Resource configLocation) {
		try {
			URL configUrl = configLocation.getURL();
			String configFileName = configUrl.getPath().toLowerCase(Locale.ROOT);
			return (!isYaml(configFileName)) ? new XmlClientConfigBuilder(configUrl).build()
					: new YamlClientConfigBuilder(configUrl).build();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load Hazelcast config", ex);
		}
	}

	private boolean isYaml(String configFileName) {
		return configFileName.endsWith(".yaml") || configFileName.endsWith(".yml");
	}

}
