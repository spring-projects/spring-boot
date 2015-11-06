/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import javax.cache.Caching;

import com.hazelcast.core.IMap;
import com.hazelcast.spring.cache.HazelcastCache;
import net.sf.ehcache.Ehcache;
import org.infinispan.spring.provider.SpringCache;

import org.springframework.boot.actuate.cache.CacheStatistics;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.cache.ConcurrentMapCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.DefaultCacheStatistics;
import org.springframework.boot.actuate.cache.EhCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.GuavaCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.HazelcastCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.InfinispanCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.JCacheCacheStatisticsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link CacheStatisticsProvider}
 * beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.3.0
 */
@Configuration
@AutoConfigureAfter(CacheAutoConfiguration.class)
@ConditionalOnBean(CacheManager.class)
public class CacheStatisticsAutoConfiguration {

	@Configuration
	@ConditionalOnClass({ Caching.class, JCacheCache.class })
	static class JCacheCacheStatisticsProviderConfiguration {

		@Bean
		public JCacheCacheStatisticsProvider jCacheCacheStatisticsProvider() {
			return new JCacheCacheStatisticsProvider();
		}

	}

	@Configuration
	@ConditionalOnClass({ EhCacheCache.class, Ehcache.class })
	static class EhCacheCacheStatisticsProviderConfiguration {

		@Bean
		public EhCacheStatisticsProvider ehCacheCacheStatisticsProvider() {
			return new EhCacheStatisticsProvider();
		}

	}

	@Configuration
	@ConditionalOnClass({ IMap.class, HazelcastCache.class })
	static class HazelcastCacheStatisticsConfiguration {

		@Bean
		public HazelcastCacheStatisticsProvider hazelcastCacheStatisticsProvider() {
			return new HazelcastCacheStatisticsProvider();
		}
	}

	@Configuration
	@ConditionalOnClass({ SpringCache.class })
	static class InfinispanCacheStatisticsProviderConfiguration {

		@Bean
		public InfinispanCacheStatisticsProvider infinispanCacheStatisticsProvider() {
			return new InfinispanCacheStatisticsProvider();
		}

	}

	@Configuration
	@ConditionalOnClass({ com.google.common.cache.Cache.class, GuavaCache.class })
	static class GuavaCacheStatisticsConfiguration {

		@Bean
		public GuavaCacheStatisticsProvider guavaCacheStatisticsProvider() {
			return new GuavaCacheStatisticsProvider();
		}

	}

	@Configuration
	@ConditionalOnClass(ConcurrentMapCache.class)
	static class ConcurrentMapCacheStatisticsConfiguration {

		@Bean
		public ConcurrentMapCacheStatisticsProvider concurrentMapCacheStatisticsProvider() {
			return new ConcurrentMapCacheStatisticsProvider();
		}

	}

	@Configuration
	@ConditionalOnClass(NoOpCacheManager.class)
	static class NoOpCacheStatisticsConfiguration {

		private static final CacheStatistics NO_OP_STATS = new DefaultCacheStatistics();

		@Bean
		public CacheStatisticsProvider<Cache> noOpCacheStatisticsProvider() {
			return new CacheStatisticsProvider<Cache>() {
				@Override
				public CacheStatistics getCacheStatistics(CacheManager cacheManager,
						Cache cache) {
					if (cacheManager instanceof NoOpCacheManager) {
						return NO_OP_STATS;
					}
					return null;
				}
			};
		}

	}

}
