/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheMetricsConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class CacheMetricsConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(RegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
			.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	public void autoConfiguredCacheManagerIsInstrumented() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
				.withPropertyValues("spring.cache.type=caffeine",
						"spring.cache.cache-names=cache1,cache2")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("cache.requests").tags("name", "cache1")
							.tags("cacheManager", "cacheManager").meter()).isPresent();
					assertThat(registry.find("cache.requests").tags("name", "cache2")
							.tags("cacheManager", "cacheManager").meter()).isPresent();
				});
	}

	@Test
	public void autoConfiguredCacheManagerWithCustomMetricName() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
				.withPropertyValues(
						"management.metrics.cache.cache-metric-name=custom.name",
						"spring.cache.type=caffeine", "spring.cache.cache-names=cache1")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(
							registry.find("custom.name.requests").tags("name", "cache1")
									.tags("cacheManager", "cacheManager").meter())
											.isPresent();
				});
	}

	@Test
	public void autoConfiguredNonSupportedCacheManagerIsIgnored() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
				.withPropertyValues("spring.cache.type=simple",
						"spring.cache.cache-names=cache1,cache2")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("cache.requests").tags("name", "cache1")
							.tags("cacheManager", "cacheManager").meter()).isNotPresent();
					assertThat(registry.find("cache.requests").tags("name", "cache2")
							.tags("cacheManager", "cacheManager").meter()).isNotPresent();
				});
	}

	@Test
	public void cacheInstrumentationCanBeDisabled() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
				.withPropertyValues("management.metrics.cache.instrument-cache=false",
						"spring.cache.type=caffeine", "spring.cache.cache-names=cache1")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("cache.requests").tags("name", "cache1")
							.tags("cacheManager", "cacheManager").meter()).isNotPresent();
				});
	}

	@Configuration
	@EnableCaching
	static class RegistryConfiguration {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}
