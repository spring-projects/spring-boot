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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

/**
 * Hazelcast cache configuration. Only kick in if a configuration file location is set or
 * if a default configuration file exists (either placed in the default location or set
 * via the {@value #CONFIG_SYSTEM_PROPERTY} system property).
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ HazelcastInstance.class, HazelcastCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional({ CacheCondition.class,
		HazelcastCacheConfiguration.ConfigAvailableCondition.class })
class HazelcastCacheConfiguration {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.config";

	@Autowired
	private CacheProperties cacheProperties;

	@Bean
	public HazelcastCacheManager cacheManager(HazelcastInstance hazelcastInstance) {
		return new HazelcastCacheManager(hazelcastInstance);
	}

	@Bean
	@ConditionalOnMissingBean
	public HazelcastInstance hazelcastInstance() throws IOException {
		Resource location = this.cacheProperties
				.resolveConfigLocation(this.cacheProperties.getHazelcast().getConfig());
		if (location != null) {
			Config cfg = new XmlConfigBuilder(location.getURL()).build();
			return Hazelcast.newHazelcastInstance(cfg);
		}
		return Hazelcast.newHazelcastInstance();
	}

	/**
	 * Determines if the Hazelcast configuration is available. This either kicks in if a
	 * default configuration has been found or if property referring to the file to use
	 * has been set.
	 */
	static class ConfigAvailableCondition extends CacheConfigFileCondition {

		public ConfigAvailableCondition() {
			super("Hazelcast", "spring.cache.hazelcast", "file:./hazelcast.xml",
					"classpath:/hazelcast.xml");
		}

		@Override
		protected ConditionOutcome getResourceOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (System.getProperty(CONFIG_SYSTEM_PROPERTY) != null) {
				return ConditionOutcome.match("System property '"
						+ CONFIG_SYSTEM_PROPERTY + "' is set.");
			}
			return super.getResourceOutcome(context, metadata);
		}

	}

}
