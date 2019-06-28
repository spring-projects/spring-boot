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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory that can be used to create a client {@link HazelcastInstance}.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
public class HazelcastClientFactory {

	private final ClientConfig clientConfig;

	/**
	 * Create a {@link HazelcastClientFactory} for the specified configuration location.
	 * @param clientConfigLocation the location of the configuration file
	 * @throws IOException if the configuration location could not be read
	 */
	public HazelcastClientFactory(Resource clientConfigLocation) throws IOException {
		this.clientConfig = getClientConfig(clientConfigLocation);
	}

	/**
	 * Create a {@link HazelcastClientFactory} for the specified configuration.
	 * @param clientConfig the configuration
	 */
	public HazelcastClientFactory(ClientConfig clientConfig) {
		Assert.notNull(clientConfig, "ClientConfig must not be null");
		this.clientConfig = clientConfig;
	}

	private ClientConfig getClientConfig(Resource clientConfigLocation) throws IOException {
		URL configUrl = clientConfigLocation.getURL();
		String configFileName = configUrl.getPath();
		if (configFileName.endsWith(".yaml")) {
			return new YamlClientConfigBuilder(configUrl).build();
		}
		return new XmlClientConfigBuilder(configUrl).build();
	}

	/**
	 * Get the {@link HazelcastInstance}.
	 * @return the {@link HazelcastInstance}
	 */
	public HazelcastInstance getHazelcastInstance() {
		if (StringUtils.hasText(this.clientConfig.getInstanceName())) {
			return HazelcastClient.getHazelcastClientByName(this.clientConfig.getInstanceName());
		}
		return HazelcastClient.newHazelcastClient(this.clientConfig);
	}

}
