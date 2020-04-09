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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ClientConfig.class)
	@Conditional(ConfigAvailableCondition.class)
	static class HazelcastClientConfigFileConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(HazelcastProperties properties) throws IOException {
			Resource config = properties.resolveConfigLocation();
			if (config != null) {
				return new HazelcastClientFactory(config).getHazelcastInstance();
			}
			return HazelcastClient.newHazelcastClient();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(ClientConfig.class)
	static class HazelcastClientConfigConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(ClientConfig config) {
			return new HazelcastClientFactory(config).getHazelcastInstance();
		}

	}

	/**
	 * {@link HazelcastConfigResourceCondition} that checks if the
	 * {@code spring.hazelcast.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends HazelcastConfigResourceCondition {

		ConfigAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY, "file:./hazelcast-client.xml", "classpath:/hazelcast-client.xml",
					"file:./hazelcast-client.yaml", "classpath:/hazelcast-client.yaml");
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String springHazelcastConfigProperty = "spring.hazelcast.config";
			if (context.getEnvironment().containsProperty(springHazelcastConfigProperty)) {
				String configLocation = context.getEnvironment().getProperty(springHazelcastConfigProperty);
				Resource resource = context.getResourceLoader().getResource(configLocation);
				if (resource.exists()) {
					try {
						validateHazelcastClientConfig(resource);
						return ConditionOutcome.match(startConditionMessage().foundExactly("property "
								+ springHazelcastConfigProperty));
					} catch (Exception e) {
						return super.getResourceOutcome(context, metadata);
					}
				}
			}
			return super.getResourceOutcome(context, metadata);
		}

		private void validateHazelcastClientConfig(Resource resource)
				throws IOException {
			URL configUrl = resource.getURL();
			String configFileName = configUrl.getPath();
			if (configFileName.endsWith(".yaml")) {
				new YamlClientConfigBuilder(configUrl).build();
			} else if (configFileName.endsWith(".xml")) {
				new XmlClientConfigBuilder(configUrl).build();
			}
		}
	}
}
