/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cache.autoconfigure.metrics;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.cache.metrics.CacheMeterBinderProvider;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.boot.cache.metrics.LazyCacheMetricsRegistrar;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CacheMetricsRegistrarConfiguration}.
 *
 * @author Spring Boot Team
 */
class CacheMetricsRegistrarConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void lazyCacheMetricsRegistrarIsCreatedByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CacheMetricsRegistrar.class);
			CacheMetricsRegistrar registrar = context.getBean(CacheMetricsRegistrar.class);
			assertThat(registrar).isInstanceOf(LazyCacheMetricsRegistrar.class);
		});
	}

	@Test
	void eagerRegistrationWhenLazyRegistrationDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.cache.lazy-registration=false")
				.run((context) -> {
					assertThat(context).hasSingleBean(CacheMetricsRegistrar.class);
					CacheMetricsRegistrar registrar = context.getBean(CacheMetricsRegistrar.class);
					assertThat(registrar).isInstanceOf(LazyCacheMetricsRegistrar.class);
					
					// Verify that eager registration listener is created
					assertThat(context).hasBean("eagerCacheRegistrationListener");
				});
	}

	@Test
	void cacheManagerNameResolution() {
		this.contextRunner.run((context) -> {
			CacheMetricsRegistrarConfiguration config = context.getBean(CacheMetricsRegistrarConfiguration.class);
			
			// Test cache manager name resolution
			CacheManager cacheManager = context.getBean(CacheManager.class);
			cacheManager.getCache("test"); // Create a cache
			
			String cacheManagerName = config.getCacheManagerNameForCache(cacheManager.getCache("test"));
			assertThat(cacheManagerName).isNotNull();
		});
	}

	@Test
	void multipleCacheManagersHandling() {
		this.contextRunner
				.withUserConfiguration(MultipleCacheManagersConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(CacheMetricsRegistrar.class);
					assertThat(context).hasBean("cacheManager1");
					assertThat(context).hasBean("cacheManager2");
					
					CacheMetricsRegistrarConfiguration config = context.getBean(CacheMetricsRegistrarConfiguration.class);
					assertThat(config).isNotNull();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("test", "cache1", "cache2");
		}

		@Bean
		CacheMeterBinderProvider<?> cacheMeterBinderProvider() {
			return (cache, tags) -> (registry) -> {};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleCacheManagersConfiguration extends TestConfiguration {

		@Bean
		CacheManager cacheManager1() {
			return new ConcurrentMapCacheManager("cache1");
		}

		@Bean
		CacheManager cacheManager2() {
			return new ConcurrentMapCacheManager("cache2");
		}
	}
}