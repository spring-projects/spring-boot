/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.io.IOException;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Hazelcast cache configuration. Only kick in if a configuration file location
 * is set or if a default configuration file exists (either placed in the default
 * location or set via the {@value #CONFIG_SYSTEM_PROPERTY} system property).
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnMissingBean(CacheManager.class)
@ConditionalOnClass({HazelcastInstance.class, HazelcastCacheManager.class})
@Conditional(HazelcastConfiguration.ConfigAvailableCondition.class)
@ConditionalOnProperty(prefix = "spring.cache", value = "mode", havingValue = "hazelcast", matchIfMissing = true)
class HazelcastConfiguration {


	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.config";

	@Autowired
	private CacheProperties cacheProperties;

	@Bean
	public HazelcastCacheManager cacheManager() throws IOException {
		return new HazelcastCacheManager(createHazelcastInstance());
	}

	private HazelcastInstance createHazelcastInstance() throws IOException {
		Resource location = this.cacheProperties.resolveConfigLocation();
		if (location != null) {
			Config cfg = new XmlConfigBuilder(location.getURL()).build();
			return Hazelcast.newHazelcastInstance(cfg);
		}
		else {
			return Hazelcast.newHazelcastInstance();
		}
	}


	/**
	 * Determines if the Hazelcast configuration is available. This either kick in if a default
	 * configuration has been found or if property referring to the file to use has been set.
	 */
	static class ConfigAvailableCondition extends AnyNestedCondition {

		public ConfigAvailableCondition() {
			super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.cache", name = "config")
		static class CacheLocationProperty {
		}

		@Conditional(BootstrapConfigurationAvailableCondition.class)
		static class DefaultConfigurationAvailable {
		}

	}

	static class BootstrapConfigurationAvailableCondition extends SpringBootCondition {

		private final ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (System.getProperty(CONFIG_SYSTEM_PROPERTY) != null) {
				return ConditionOutcome.match("System property '" + CONFIG_SYSTEM_PROPERTY + "' is set.");
			}
			if (resourceLoader.getResource("file:./hazelcast.xml").exists()) {
				return ConditionOutcome.match("hazelcast.xml found in the working directory.");
			}
			if (resourceLoader.getResource("classpath:/hazelcast.xml").exists()) {
				return ConditionOutcome.match("hazelcast.xml found in the classpath.");
			}
			return ConditionOutcome.noMatch("No hazelcast.xml file found and system property '"
					+ CONFIG_SYSTEM_PROPERTY + "' is not set.");
		}
	}

}
