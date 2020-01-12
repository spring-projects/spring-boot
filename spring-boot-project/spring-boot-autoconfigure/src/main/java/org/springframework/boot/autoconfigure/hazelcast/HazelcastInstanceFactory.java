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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.config.XmlConfigLocator;
import com.hazelcast.config.YamlConfigBuilder;
import com.hazelcast.config.YamlConfigLocator;
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
 * @since 1.3.0
 */
public class HazelcastInstanceFactory {

	private final Config config;

	/**
	 * Create a new {@link HazelcastInstanceFactory} instance.
	 * @param configCustomizers any {@link HazelcastConfigCustomizer customizers} that
	 * should be applied when the {@link Config} is built and before
	 * {@link HazelcastInstance} is created.
	 * @since 2.3.0
	 */
	public HazelcastInstanceFactory(HazelcastConfigCustomizer... configCustomizers) {
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		Config config = getDefaultConfig();
		for (HazelcastConfigCustomizer configCustomizer : configCustomizers) {
			configCustomizer.customize(config);
		}
		this.config = config;
	}

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration location.
	 * @param configLocation the location of the configuration file
	 * @param configCustomizers any {@link HazelcastConfigCustomizer customizers} that
	 * should be applied when the {@link Config} is built and before
	 * {@link HazelcastInstance} is created.
	 * @throws IOException if the configuration location could not be read
	 */
	public HazelcastInstanceFactory(Resource configLocation, HazelcastConfigCustomizer... configCustomizers)
			throws IOException {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		Config config = getConfig(configLocation);
		for (HazelcastConfigCustomizer configCustomizer : configCustomizers) {
			configCustomizer.customize(config);
		}
		this.config = config;
	}

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration.
	 * @param config the configuration
	 */
	public HazelcastInstanceFactory(Config config) {
		Assert.notNull(config, "Config must not be null");
		this.config = config;
	}

	private Config getConfig(Resource configLocation) throws IOException {
		URL configUrl = configLocation.getURL();
		Config config = createConfig(configUrl);
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(configLocation.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		return config;
	}

	private static Config createConfig(URL configUrl) throws IOException {
		String configFileName = configUrl.getPath();
		if (configFileName.endsWith(".yaml")) {
			return new YamlConfigBuilder(configUrl).build();
		}
		return new XmlConfigBuilder(configUrl).build();
	}

	private Config getDefaultConfig() {
		XmlConfigLocator xmlConfigLocator = new XmlConfigLocator();
		YamlConfigLocator yamlConfigLocator = new YamlConfigLocator();
		if (yamlConfigLocator.locateFromSystemProperty()) {
			return new YamlConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateFromSystemProperty()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		if (xmlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		if (yamlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new YamlConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateDefault()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		throw new IllegalStateException("Hazelcast Config does not exist");
	}

	/**
	 * Get the {@link HazelcastInstance}.
	 * @return the {@link HazelcastInstance}
	 */
	public HazelcastInstance getHazelcastInstance() {
		if (StringUtils.hasText(this.config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(this.config);
		}
		return Hazelcast.newHazelcastInstance(this.config);
	}

}
