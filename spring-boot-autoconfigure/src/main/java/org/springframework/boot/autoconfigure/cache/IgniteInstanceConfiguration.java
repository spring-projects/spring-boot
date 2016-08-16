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

package org.springframework.boot.autoconfigure.cache;

import java.io.IOException;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.spring.SpringCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.ignite.IgniteConfigResourceCondition;
import org.springframework.boot.autoconfigure.ignite.IgniteFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Actual {@link Ignite} configurations imported by {@link IgniteCacheConfiguration}.
 *
 * @author wmz7year
 * @since 1.4.1
 */
abstract class IgniteInstanceConfiguration {

	@Configuration
	@ConditionalOnSingleCandidate(Ignite.class)
	static class Existing {

		private final CacheProperties cacheProperties;

		private final CacheManagerCustomizers customizers;

		Existing(CacheProperties cacheProperties, CacheManagerCustomizers customizers) {
			this.cacheProperties = cacheProperties;
			this.customizers = customizers;
		}

		@Bean
		public SpringCacheManager cacheManager(Ignite existingIgniteInstance)
				throws IOException {
			SpringCacheManager springCacheManager = new SpringCacheManager();
			springCacheManager.setGridName(existingIgniteInstance.name());
			Resource config = this.cacheProperties.getIgnite().getConfig();
			Resource location = this.cacheProperties.resolveConfigLocation(config);
			if (location != null) {
				springCacheManager
						.setConfigurationPath(location.getFile().getAbsolutePath());
			}

			return this.customizers.customize(springCacheManager);
		}
	}

	@Configuration
	@ConditionalOnMissingBean(Ignite.class)
	@Conditional(ConfigAvailableCondition.class)
	static class Specific {

		private final CacheProperties cacheProperties;

		private final CacheManagerCustomizers customizers;

		Specific(CacheProperties cacheProperties, CacheManagerCustomizers customizers) {
			this.cacheProperties = cacheProperties;
			this.customizers = customizers;
		}

		@Bean
		public Ignite igniteInstance() throws IOException {
			Resource config = this.cacheProperties.getIgnite().getConfig();
			Resource location = this.cacheProperties.resolveConfigLocation(config);
			if (location != null) {
				return Ignition.start(location.getURL());
			}

			return new IgniteFactory().getIgniteInstance();
		}

		@Bean
		public SpringCacheManager cacheManager() throws IOException {
			Ignite igniteInstance = igniteInstance();
			SpringCacheManager springCacheManager = new SpringCacheManager();
			springCacheManager.setGridName(igniteInstance.name());
			return this.customizers.customize(springCacheManager);
		}
	}

	/**
	 * {@link IgniteConfigResourceCondition} that checks if the
	 * {@code spring.cache.ignite.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends IgniteConfigResourceCondition {

		ConfigAvailableCondition() {
			super("spring.cache.ignite", "config");
		}

	}
}
