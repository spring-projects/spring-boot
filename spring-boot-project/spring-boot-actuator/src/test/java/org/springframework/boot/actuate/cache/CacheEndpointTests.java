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

package org.springframework.boot.actuate.cache;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheEndpoint}.
 *
 * @author Johannes Edmeier
 */
public class CacheEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

	@Test
	public void cacheReportIsReturned() {
		this.contextRunner.withUserConfiguration(Config.class)
				.run((context) -> assertThat(context.getBean(CacheEndpoint.class)
						.cacheManagerBeans().getContexts().get(context.getId())
						.getCacheManagerBeans().get("cacheManager").getCacheNames())
								.containsOnly("first", "second"));
	}

	@Test
	public void cacheIsCleared() {
		this.contextRunner.withUserConfiguration(Config.class).run((context) -> {
			Cache firstCache = context.getBean("firstCache", Cache.class);
			firstCache.put("key", "vale");
			Cache secondCache = context.getBean("secondCache", Cache.class);
			secondCache.put("key", "value");
			context.getBean(CacheEndpoint.class).clearCaches(null, null, null);
			assertThat(firstCache.get("key", String.class)).isNull();
			assertThat(secondCache.get("key", String.class)).isNull();
		});
	}

	@Test
	public void namedCacheIsCleared() {
		this.contextRunner.withUserConfiguration(Config.class).run((context) -> {
			Cache firstCache = context.getBean("firstCache", Cache.class);
			firstCache.put("key", "vale");
			Cache secondCache = context.getBean("secondCache", Cache.class);
			secondCache.put("key", "value");
			context.getBean(CacheEndpoint.class).clearCaches(context.getId(), null,
					"first");
			assertThat(firstCache.get("key", String.class)).isNull();
			assertThat(secondCache.get("key", String.class)).isEqualTo("value");
		});
	}

	@Configuration
	@EnableCaching
	public static class Config {
		@Bean
		public Cache firstCache() {
			return new ConcurrentMapCache("first");
		}

		@Bean
		public Cache secondCache() {
			return new ConcurrentMapCache("second");
		}

		@Bean
		public CacheEndpoint endpoint(ApplicationContext context) {
			return new CacheEndpoint(context);
		}

	}

}
