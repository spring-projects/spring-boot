/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.ignite;

import java.io.IOException;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache ignite. Creates a
 * {@link Ignite} based on explicit configuration or when a default configuration file is
 * found in the environment.
 *
 * @author wmz7year
 * @since 1.4.1
 * @see IgniteConfigResourceCondition
 */
@Configuration
@ConditionalOnClass(Ignite.class)
@ConditionalOnMissingBean(Ignite.class)
@EnableConfigurationProperties(IgniteProperties.class)
public class IgniteAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(IgniteConfiguration.class)
	@Conditional(ConfigAvailableCondition.class)
	static class IgniteConfigFileConfiguration {

		private final IgniteProperties igniteProperties;

		IgniteConfigFileConfiguration(IgniteProperties igniteProperties) {
			this.igniteProperties = igniteProperties;
		}

		@Bean
		public Ignite igniteInstance() throws IOException {
			Resource config = this.igniteProperties.resolveConfigLocation();
			if (config != null) {
				return Ignition.start(config.getURL());
			}
			return new IgniteFactory().getIgniteInstance();
		}
	}

	@Configuration
	@ConditionalOnSingleCandidate(IgniteConfiguration.class)
	static class IgniteConfigConfiguration {

		public Ignite igniteInstance(IgniteConfiguration config) {
			return new IgniteFactory(config).getIgniteInstance();
		}
	}

	/**
	 * {@link IgniteConfigResourceCondition} that checks if the
	 * {@code spring.ignite.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends IgniteConfigResourceCondition {

		ConfigAvailableCondition() {
			super("spring.ignite", "config");
		}

	}
}
