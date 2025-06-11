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
import java.util.List;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class CacheMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class)
		.withUserConfiguration(CachingConfiguration.class)
		.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, CacheMetricsAutoConfiguration.class));

	@Test
	void autoConfiguredCache2kIsInstrumented() {
		this.contextRunner.withPropertyValues("spring.cache.type=cache2k", "spring.cache.cache-names=cache1,cache2")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("cache.gets").tags("name", "cache1").tags("cache.manager", "cacheManager").meter();
				registry.get("cache.gets").tags("name", "cache2").tags("cache.manager", "cacheManager").meter();
			});
	}

	@Test
	void autoConfiguredCacheManagerIsInstrumented() {
		this.contextRunner
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cache-names=cache1,cache2",
					"spring.cache.caffeine.spec=recordStats")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("cache.gets").tags("name", "cache1").tags("cache.manager", "cacheManager").meter();
				registry.get("cache.gets").tags("name", "cache2").tags("cache.manager", "cacheManager").meter();
			});
	}

	@Test
	void autoConfiguredNonSupportedCacheManagerIsIgnored() {
		this.contextRunner.withPropertyValues("spring.cache.type=simple", "spring.cache.cache-names=cache1,cache2")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("cache.gets")
					.tags("name", "cache1")
					.tags("cache.manager", "cacheManager")
					.meter()).isNull();
				assertThat(registry.find("cache.gets")
					.tags("name", "cache2")
					.tags("cache.manager", "cacheManager")
					.meter()).isNull();
			});
	}

	@Test
	void cacheInstrumentationCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("management.metrics.enable.cache=false", "spring.cache.type=caffeine",
					"spring.cache.cache-names=cache1")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("cache.requests")
					.tags("name", "cache1")
					.tags("cache.manager", "cacheManager")
					.meter()).isNull();
			});
	}

	@Test
	void customCacheManagersAreInstrumented() {
		this.contextRunner
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cache-names=cache1,cache2",
					"spring.cache.caffeine.spec=recordStats")
			.withUserConfiguration(CustomCacheManagersConfiguration.class)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("cache.gets").meters()).map((meter) -> meter.getId().getTag("cache"))
					.containsOnly("standard", "non-default");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCacheManagersConfiguration implements CachingConfigurer {

		@Bean
		CacheManager standardCacheManager() {
			CaffeineCacheManager cacheManager = new CaffeineCacheManager();
			cacheManager.setCaffeineSpec(CaffeineSpec.parse("recordStats"));
			cacheManager.setCacheNames(List.of("standard"));
			return cacheManager;
		}

		@Bean(defaultCandidate = false)
		CacheManager nonDefaultCacheManager() {
			CaffeineCacheManager cacheManager = new CaffeineCacheManager();
			cacheManager.setCaffeineSpec(CaffeineSpec.parse("recordStats"));
			cacheManager.setCacheNames(List.of("non-default"));
			return cacheManager;
		}

		@Bean(autowireCandidate = false)
		CacheManager nonAutowireCacheManager() {
			CaffeineCacheManager cacheManager = new CaffeineCacheManager();
			cacheManager.setCaffeineSpec(CaffeineSpec.parse("recordStats"));
			cacheManager.setCacheNames(List.of("non-autowire"));
			return cacheManager;
		}

		@Bean
		@Override
		public CacheResolver cacheResolver() {
			return (context) -> Collections.emptyList();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CachingConfiguration {

	}

}
