/*
 * Copyright 2012-2023 the original author or authors.
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
import java.io.InputStream;
import java.net.URL;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration for Hazelcast server.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(HazelcastInstance.class)
class HazelcastServerConfiguration {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.config";

	static final String HAZELCAST_LOGGING_TYPE = "hazelcast.logging.type";

	private static HazelcastInstance getHazelcastInstance(Config config) {
		if (StringUtils.hasText(config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(config);
		}
		return Hazelcast.newHazelcastInstance(config);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Config.class)
	@Conditional(ConfigAvailableCondition.class)
	static class HazelcastServerConfigFileConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(HazelcastProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<HazelcastConfigCustomizer> hazelcastConfigCustomizers) throws IOException {
			Resource configLocation = properties.resolveConfigLocation();
			Config config = (configLocation != null) ? loadConfig(configLocation) : Config.load();
			config.setClassLoader(resourceLoader.getClassLoader());
			hazelcastConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(config));
			return getHazelcastInstance(config);
		}

		private Config loadConfig(Resource configLocation) throws IOException {
			URL configUrl = configLocation.getURL();
			Config config = loadConfig(configUrl);
			if (ResourceUtils.isFileURL(configUrl)) {
				config.setConfigurationFile(configLocation.getFile());
			}
			else {
				config.setConfigurationUrl(configUrl);
			}
			return config;
		}

		private Config loadConfig(URL configUrl) throws IOException {
			try (InputStream stream = configUrl.openStream()) {
				return Config.loadFromStream(stream);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(Config.class)
	static class HazelcastServerConfigConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(Config config) {
			return getHazelcastInstance(config);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SpringManagedContext.class)
	static class SpringManagedContextHazelcastConfigCustomizerConfiguration {

		@Bean
		@Order(0)
		HazelcastConfigCustomizer springManagedContextHazelcastConfigCustomizer(ApplicationContext applicationContext) {
			return (config) -> {
				SpringManagedContext managementContext = new SpringManagedContext();
				managementContext.setApplicationContext(applicationContext);
				config.setManagedContext(managementContext);
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.slf4j.Logger.class)
	static class HazelcastLoggingConfigCustomizerConfiguration {

		@Bean
		@Order(0)
		HazelcastConfigCustomizer loggingHazelcastConfigCustomizer() {
			return (config) -> {
				if (!config.getProperties().containsKey(HAZELCAST_LOGGING_TYPE)) {
					config.setProperty(HAZELCAST_LOGGING_TYPE, "slf4j");
				}
			};
		}

	}

	/**
	 * {@link HazelcastConfigResourceCondition} that checks if the
	 * {@code spring.hazelcast.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends HazelcastConfigResourceCondition {

		ConfigAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY, "file:./hazelcast.xml", "classpath:/hazelcast.xml", "file:./hazelcast.yaml",
					"classpath:/hazelcast.yaml", "file:./hazelcast.yml", "classpath:/hazelcast.yml");
		}

	}

}
