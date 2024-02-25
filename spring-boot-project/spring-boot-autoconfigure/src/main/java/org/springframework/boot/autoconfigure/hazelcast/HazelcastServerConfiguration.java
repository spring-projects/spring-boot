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

	/**
	 * Returns a HazelcastInstance based on the provided configuration. If the
	 * configuration has an instance name, it will return an existing HazelcastInstance
	 * with that name, otherwise it will create a new HazelcastInstance using the provided
	 * configuration.
	 * @param config the configuration for the HazelcastInstance
	 * @return the HazelcastInstance based on the provided configuration
	 */
	private static HazelcastInstance getHazelcastInstance(Config config) {
		if (StringUtils.hasText(config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(config);
		}
		return Hazelcast.newHazelcastInstance(config);
	}

	/**
	 * HazelcastServerConfigFileConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Config.class)
	@Conditional(ConfigAvailableCondition.class)
	static class HazelcastServerConfigFileConfiguration {

		/**
		 * Creates and returns a Hazelcast instance based on the provided configuration
		 * properties.
		 * @param properties the Hazelcast properties
		 * @param resourceLoader the resource loader
		 * @param hazelcastConfigCustomizers the customizers for the Hazelcast
		 * configuration
		 * @return the Hazelcast instance
		 * @throws IOException if an I/O error occurs while loading the configuration
		 */
		@Bean
		HazelcastInstance hazelcastInstance(HazelcastProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<HazelcastConfigCustomizer> hazelcastConfigCustomizers) throws IOException {
			Resource configLocation = properties.resolveConfigLocation();
			Config config = (configLocation != null) ? loadConfig(configLocation) : Config.load();
			config.setClassLoader(resourceLoader.getClassLoader());
			hazelcastConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(config));
			return getHazelcastInstance(config);
		}

		/**
		 * Loads the configuration from the given resource location.
		 * @param configLocation the resource location of the configuration file
		 * @return the loaded configuration
		 * @throws IOException if an I/O error occurs while loading the configuration
		 */
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

		/**
		 * Loads the configuration from the specified URL.
		 * @param configUrl the URL of the configuration file
		 * @return the loaded configuration
		 * @throws IOException if an I/O error occurs while reading the configuration file
		 */
		private Config loadConfig(URL configUrl) throws IOException {
			try (InputStream stream = configUrl.openStream()) {
				return Config.loadFromStream(stream);
			}
		}

	}

	/**
	 * HazelcastServerConfigConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(Config.class)
	static class HazelcastServerConfigConfiguration {

		/**
		 * Creates and returns a HazelcastInstance using the provided configuration.
		 * @param config the configuration to be used for creating the HazelcastInstance
		 * @return the created HazelcastInstance
		 */
		@Bean
		HazelcastInstance hazelcastInstance(Config config) {
			return getHazelcastInstance(config);
		}

	}

	/**
	 * SpringManagedContextHazelcastConfigCustomizerConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SpringManagedContext.class)
	static class SpringManagedContextHazelcastConfigCustomizerConfiguration {

		/**
		 * Returns a HazelcastConfigCustomizer bean that sets up a SpringManagedContext
		 * for the Hazelcast configuration. The SpringManagedContext is configured with
		 * the provided ApplicationContext. This allows Hazelcast to access Spring beans
		 * and manage their lifecycle within the Hazelcast cluster.
		 * @param applicationContext the ApplicationContext to be set in the
		 * SpringManagedContext
		 * @return the HazelcastConfigCustomizer bean with the configured
		 * SpringManagedContext
		 */
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

	/**
	 * HazelcastLoggingConfigCustomizerConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.slf4j.Logger.class)
	static class HazelcastLoggingConfigCustomizerConfiguration {

		/**
		 * Returns a HazelcastConfigCustomizer that sets the logging type to "slf4j" if it
		 * is not already set. This customizer is ordered with a priority of 0.
		 * @return the HazelcastConfigCustomizer for logging configuration
		 */
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

		/**
		 * Initializes a new instance of the ConfigAvailableCondition class.
		 * @param systemProperty the system property to check for the configuration file
		 * path
		 * @param fileConfigPaths the file paths to check for the configuration file
		 * @param classpathConfigPaths the classpath paths to check for the configuration
		 * file
		 */
		ConfigAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY, "file:./hazelcast.xml", "classpath:/hazelcast.xml", "file:./hazelcast.yaml",
					"classpath:/hazelcast.yaml", "file:./hazelcast.yml", "classpath:/hazelcast.yml");
		}

	}

}
