/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.cache.CouchbaseCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link CacheAutoConfiguration} tests.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractCacheAutoConfigurationTests {

	protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

	protected <T extends CacheManager> T getCacheManager(AssertableApplicationContext loaded, Class<T> type) {
		CacheManager cacheManager = loaded.getBean(CacheManager.class);
		assertThat(cacheManager).as("Wrong cache manager type").isInstanceOf(type);
		return type.cast(cacheManager);
	}

	@SuppressWarnings("rawtypes")
	protected ContextConsumer<AssertableApplicationContext> verifyCustomizers(String... expectedCustomizerNames) {
		return (context) -> {
			CacheManager cacheManager = getCacheManager(context, CacheManager.class);
			List<String> expected = new ArrayList<>(Arrays.asList(expectedCustomizerNames));
			Map<String, CacheManagerTestCustomizer> customizer = context
				.getBeansOfType(CacheManagerTestCustomizer.class);
			customizer.forEach((key, value) -> {
				if (expected.contains(key)) {
					expected.remove(key);
					assertThat(value.cacheManager).isSameAs(cacheManager);
				}
				else {
					assertThat(value.cacheManager).isNull();
				}
			});
			assertThat(expected).isEmpty();
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class CacheManagerCustomizersConfiguration {

		@Bean
		CacheManagerCustomizer<CacheManager> allCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<ConcurrentMapCacheManager> simpleCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<SimpleCacheManager> genericCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<CouchbaseCacheManager> couchbaseCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<RedisCacheManager> redisCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<HazelcastCacheManager> hazelcastCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<SpringEmbeddedCacheManager> infinispanCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<SpringCache2kCacheManager> cache2kCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

		@Bean
		CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<>() {

			};
		}

	}

	abstract static class CacheManagerTestCustomizer<T extends CacheManager> implements CacheManagerCustomizer<T> {

		T cacheManager;

		@Override
		public void customize(T cacheManager) {
			if (this.cacheManager != null) {
				throw new IllegalStateException("Customized invoked twice");
			}
			this.cacheManager = cacheManager;
		}

	}

}
