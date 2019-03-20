/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.io.Closeable;
import java.io.IOException;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastConfigResourceCondition;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastInstanceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Actual {@link HazelcastInstance} configurations imported by
 * {@link HazelcastCacheConfiguration}.
 *
 * @author Stephane Nicoll
 */
abstract class HazelcastInstanceConfiguration {

	@Configuration
	@ConditionalOnSingleCandidate(HazelcastInstance.class)
	static class Existing {

		private final CacheProperties cacheProperties;

		private final CacheManagerCustomizers customizers;

		Existing(CacheProperties cacheProperties, CacheManagerCustomizers customizers) {
			this.cacheProperties = cacheProperties;
			this.customizers = customizers;
		}

		@Bean
		public HazelcastCacheManager cacheManager(
				HazelcastInstance existingHazelcastInstance) throws IOException {
			@SuppressWarnings("deprecation")
			Resource config = this.cacheProperties.getHazelcast().getConfig();
			Resource location = this.cacheProperties.resolveConfigLocation(config);
			if (location != null) {
				HazelcastInstance cacheHazelcastInstance = new HazelcastInstanceFactory(
						location).getHazelcastInstance();
				return new CloseableHazelcastCacheManager(cacheHazelcastInstance);
			}
			HazelcastCacheManager cacheManager = new HazelcastCacheManager(
					existingHazelcastInstance);
			return this.customizers.customize(cacheManager);
		}

	}

	@Configuration
	@ConditionalOnMissingBean(HazelcastInstance.class)
	@Conditional(ConfigAvailableCondition.class)
	static class Specific {

		private final CacheProperties cacheProperties;

		private final CacheManagerCustomizers customizers;

		Specific(CacheProperties cacheProperties, CacheManagerCustomizers customizers) {
			this.cacheProperties = cacheProperties;
			this.customizers = customizers;
		}

		@Bean
		public HazelcastInstance hazelcastInstance() throws IOException {
			@SuppressWarnings("deprecation")
			Resource config = this.cacheProperties.getHazelcast().getConfig();
			Resource location = this.cacheProperties.resolveConfigLocation(config);
			if (location != null) {
				return new HazelcastInstanceFactory(location).getHazelcastInstance();
			}
			return Hazelcast.newHazelcastInstance();
		}

		@Bean
		public HazelcastCacheManager cacheManager() throws IOException {
			HazelcastCacheManager cacheManager = new HazelcastCacheManager(
					hazelcastInstance());
			return this.customizers.customize(cacheManager);
		}

	}

	/**
	 * {@link HazelcastConfigResourceCondition} that checks if the
	 * {@code spring.cache.hazelcast.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends HazelcastConfigResourceCondition {

		ConfigAvailableCondition() {
			super("spring.cache.hazelcast", "config");
		}

	}

	private static class CloseableHazelcastCacheManager extends HazelcastCacheManager
			implements Closeable {

		private final HazelcastInstance hazelcastInstance;

		CloseableHazelcastCacheManager(HazelcastInstance hazelcastInstance) {
			super(hazelcastInstance);
			this.hazelcastInstance = hazelcastInstance;
		}

		@Override
		public void close() throws IOException {
			this.hazelcastInstance.shutdown();
		}

	}

}
