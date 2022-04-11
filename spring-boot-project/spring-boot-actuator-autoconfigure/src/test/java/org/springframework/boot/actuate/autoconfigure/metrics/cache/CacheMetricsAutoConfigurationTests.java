/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class CacheMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withUserConfiguration(CachingConfiguration.class).withConfiguration(
					AutoConfigurations.of(CacheAutoConfiguration.class, CacheMetricsAutoConfiguration.class));

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
		this.contextRunner.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cache-names=cache1,cache2")
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
					assertThat(registry.find("cache.gets").tags("name", "cache1").tags("cache.manager", "cacheManager")
							.meter()).isNull();
					assertThat(registry.find("cache.gets").tags("name", "cache2").tags("cache.manager", "cacheManager")
							.meter()).isNull();
				});
	}

	@Test
	void cacheInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.cache=false", "spring.cache.type=caffeine",
				"spring.cache.cache-names=cache1").run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("cache.requests").tags("name", "cache1")
							.tags("cache.manager", "cacheManager").meter()).isNull();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CachingConfiguration {

	}

}
