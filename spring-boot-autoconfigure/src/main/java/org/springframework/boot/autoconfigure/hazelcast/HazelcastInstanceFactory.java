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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Factory that can be used to create a {@link HazelcastInstance}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Vedran Pavic
 * @since 1.3.0
 */
public abstract class HazelcastInstanceFactory {

	/**
	 * Get the {@link HazelcastInstance}.
	 * @param config the configuration
	 * @return the {@link HazelcastInstance}
	 */
	public static HazelcastInstance createHazelcastInstance(Config config) {
		Assert.notNull(config, "Config must not be null");
		if (StringUtils.hasText(config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(config);
		}
		return Hazelcast.newHazelcastInstance(config);
	}

	/**
	 * Get the {@link HazelcastInstance}.
	 * @param configLocation the location of the configuration file
	 * @return the {@link HazelcastInstance}
	 * @throws IOException if the configuration location could not be read
	 */
	public static HazelcastInstance createHazelcastInstance(Resource configLocation)
			throws IOException {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		URL configUrl = configLocation.getURL();
		Config config = new XmlConfigBuilder(configUrl).build();
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(configLocation.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		return createHazelcastInstance(config);
	}

	/**
	 * Get the client {@link HazelcastInstance}.
	 * @param config the client configuration
	 * @return the client {@link HazelcastInstance}
	 */
	public static HazelcastInstance createHazelcastClient(ClientConfig config) {
		Assert.notNull(config, "Config must not be null");
		if (StringUtils.hasText(config.getInstanceName())) {
			return HazelcastClient.getHazelcastClientByName(config.getInstanceName());
		}
		return HazelcastClient.newHazelcastClient(config);
	}

	/**
	 * Get the client {@link HazelcastInstance}.
	 * @param configLocation the location of the client configuration file
	 * @return the client {@link HazelcastInstance}
	 * @throws IOException if the configuration location could not be read
	 */
	public static HazelcastInstance createHazelcastClient(Resource configLocation)
			throws IOException {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		URL configUrl = configLocation.getURL();
		ClientConfig config = new XmlClientConfigBuilder(configUrl).build();
		return createHazelcastClient(config);
	}

}
