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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hazelcast. Creates a
 * {@link HazelcastInstance} based on explicit configuration or when a default
 * configuration file is found in the environment.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see HazelcastConfigResourceCondition
 */
@Configuration
@ConditionalOnClass(HazelcastInstance.class)
@ConditionalOnMissingBean(HazelcastInstance.class)
@EnableConfigurationProperties(HazelcastProperties.class)
public class HazelcastAutoConfiguration {


	/**
	 * Create a {@link HazelcastInstance} based on the specified configuration location.
	 * @param location the location of the configuration file
	 * @return a {@link HazelcastInstance} for the specified configuration
	 * @throws IOException the configuration file could not be read
	 */
	public static HazelcastInstance createHazelcastInstance(Resource location)
			throws IOException {
		Assert.notNull(location, "Config must not be null");
		URL configUrl = location.getURL();
		Config config = new XmlConfigBuilder(configUrl).build();
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(location.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		return createHazelcastInstance(config);
	}

	private static HazelcastInstance createHazelcastInstance(Config config) {
		if (StringUtils.hasText(config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(config);
		}
		return Hazelcast.newHazelcastInstance(config);
	}


	@Configuration
	@ConditionalOnMissingBean({HazelcastInstance.class, Config.class})
	@Conditional(ConfigAvailableCondition.class)
	static class HazelcastConfigFileConfiguration {

		@Autowired
		private HazelcastProperties hazelcastProperties;

		@Bean
		@ConditionalOnMissingBean
		public HazelcastInstance hazelcastInstance() throws IOException {
			Resource config = this.hazelcastProperties.resolveConfigLocation();
			if (config != null) {
				return createHazelcastInstance(config);
			}
			return Hazelcast.newHazelcastInstance();
		}

	}

	@Configuration
	@ConditionalOnMissingBean(HazelcastInstance.class)
	@ConditionalOnSingleCandidate(Config.class)
	static class HazelcastConfigConfiguration {

		@Bean
		public HazelcastInstance hazelcastInstance(Config config) {
			return createHazelcastInstance(config);
		}

	}

	/**
	 * {@link HazelcastConfigResourceCondition} that checks if the
	 * {@code spring.hazelcast.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends HazelcastConfigResourceCondition {

		public ConfigAvailableCondition() {
			super("spring.hazelcast", "config");
		}
	}

}
