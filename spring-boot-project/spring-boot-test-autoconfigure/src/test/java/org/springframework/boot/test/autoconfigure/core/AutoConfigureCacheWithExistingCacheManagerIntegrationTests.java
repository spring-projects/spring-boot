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

package org.springframework.boot.test.autoconfigure.core;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureCache @AutoConfigureCache} with an existing
 * {@link CacheManager}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@AutoConfigureCache
class AutoConfigureCacheWithExistingCacheManagerIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldNotReplaceExistingCacheManager() {
		CacheManager bean = this.applicationContext.getBean(CacheManager.class);
		assertThat(bean).isInstanceOf(ConcurrentMapCacheManager.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class Config {

		@Bean
		ConcurrentMapCacheManager existingCacheManager() {
			return new ConcurrentMapCacheManager();
		}

	}

}
