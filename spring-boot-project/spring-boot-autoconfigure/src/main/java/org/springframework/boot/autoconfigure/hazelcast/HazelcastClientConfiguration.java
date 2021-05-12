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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * Configuration for Hazelcast client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HazelcastClient.class)
@ConditionalOnMissingBean(HazelcastInstance.class)
class HazelcastClientConfiguration {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.client.config";

	private static HazelcastInstance getHazelcastInstance(ClientConfig config) {
		if (StringUtils.hasText(config.getInstanceName())) {
			return HazelcastClient.getOrCreateHazelcastClient(config);
		}
		return HazelcastClient.newHazelcastClient(config);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ClientConfig.class)
	@Conditional(HazelcastClientConfigAvailableCondition.class)
	static class HazelcastClientConfigFileConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(HazelcastProperties properties, ResourceLoader resourceLoader)
				throws IOException {
			Resource configLocation = properties.resolveConfigLocation();
			ClientConfig config = (configLocation != null) ? loadClientConfig(configLocation) : ClientConfig.load();
			config.setClassLoader(resourceLoader.getClassLoader());
			return getHazelcastInstance(config);
		}

		private ClientConfig loadClientConfig(Resource configLocation) throws IOException {
			URL configUrl = configLocation.getURL();
			String configFileName = configUrl.getPath();
			if (configFileName.endsWith(".yaml")) {
				return new YamlClientConfigBuilder(configUrl).build();
			}
			return new XmlClientConfigBuilder(configUrl).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(ClientConfig.class)
	static class HazelcastClientConfigConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(ClientConfig config) {
			return getHazelcastInstance(config);
		}

	}

}
