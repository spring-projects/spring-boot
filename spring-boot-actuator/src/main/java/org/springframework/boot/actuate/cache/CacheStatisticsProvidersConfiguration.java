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

package org.springframework.boot.actuate.cache;

import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Caching;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.StatisticsGateway;

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

import com.google.common.cache.CacheStats;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.spring.cache.HazelcastCache;

/**
 * Register the {@link CacheStatisticsProvider} instances for the supported cache
 * libraries.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnBean(CacheManager.class)
public class CacheStatisticsProvidersConfiguration {

	@Configuration
	@ConditionalOnClass({ Caching.class, JCacheCache.class })
	static class JCacheCacheStatisticsProviderConfiguration {

		@Bean
		public CacheStatisticsProvider jCacheCacheStatisticsProvider() {
			return new JCacheCacheStatisticsProvider();
		}
	}

	@Configuration
	@ConditionalOnClass(Ehcache.class)
	static class EhCacheCacheStatisticsProviderConfiguration {

		@Bean
		public CacheStatisticsProvider ehCacheCacheStatisticsProvider() {
			return new CacheStatisticsProvider() {
				@Override
				public CacheStatistics getCacheStatistics(Cache cache,
						CacheManager cacheManager) {
					if (cache instanceof EhCacheCache) {
						return getEhCacheStatistics((Ehcache) cache.getNativeCache());
					}
					return null;
				}
			};
		}

		private CacheStatistics getEhCacheStatistics(Ehcache cache) {
			StatisticsGateway statistics = cache.getStatistics();
			DefaultCacheStatistics stats = new DefaultCacheStatistics();
			stats.setSize(statistics.getSize());
			Double hitRatio = statistics.cacheHitRatio();
			if (!hitRatio.isNaN()) {
				stats.setHitRatio(hitRatio);
				stats.setMissRatio(1 - hitRatio);
			}
			return stats;
		}
	}

	@Configuration
	@ConditionalOnClass(IMap.class)
	static class HazelcastCacheStatisticsConfiguration {

		@Bean
		public CacheStatisticsProvider hazelcastCacheStatisticsProvider() {
			return new CacheStatisticsProvider() {
				@Override
				public CacheStatistics getCacheStatistics(Cache cache,
						CacheManager cacheManager) {
					if (cache instanceof HazelcastCache) {
						return getHazelcastStatistics((IMap<?, ?>) cache.getNativeCache());
					}
					return null;
				}
			};
		}

		private CacheStatistics getHazelcastStatistics(IMap<?, ?> cache) {
			DefaultCacheStatistics stats = new DefaultCacheStatistics();
			LocalMapStats mapStats = cache.getLocalMapStats();
			stats.setSize(mapStats.getOwnedEntryCount());
			stats.setGetCacheCounts(mapStats.getHits(), mapStats.getGetOperationCount()
					- mapStats.getHits());
			return stats;
		}
	}

	@Configuration
	@ConditionalOnClass(com.google.common.cache.Cache.class)
	static class GuavaCacheStatisticsConfiguration {

		@Bean
		public CacheStatisticsProvider guavaCacheStatisticsProvider() {
			return new CacheStatisticsProvider() {
				@SuppressWarnings("unchecked")
				@Override
				public CacheStatistics getCacheStatistics(Cache cache,
						CacheManager cacheManager) {
					if (cache instanceof GuavaCache) {
						return getGuavaStatistics((com.google.common.cache.Cache<Object, Object>) cache
								.getNativeCache());
					}
					return null;
				}
			};
		}

		private CacheStatistics getGuavaStatistics(
				com.google.common.cache.Cache<Object, Object> cache) {
			DefaultCacheStatistics stats = new DefaultCacheStatistics();
			stats.setSize(cache.size());
			CacheStats guavaStats = cache.stats();
			if (guavaStats.requestCount() > 0) {
				stats.setHitRatio(guavaStats.hitRate());
				stats.setMissRatio(guavaStats.missRate());
			}
			return stats;
		}

	}

	@Configuration
	@ConditionalOnClass(ConcurrentMapCache.class)
	static class ConcurrentMapCacheStatisticsConfiguration {

		@Bean
		public CacheStatisticsProvider concurrentMapCacheStatisticsProvider() {
			return new CacheStatisticsProvider() {
				@Override
				public CacheStatistics getCacheStatistics(Cache cache,
						CacheManager cacheManager) {
					if (cache instanceof ConcurrentMapCache) {
						return getConcurrentMapStatistics((ConcurrentHashMap<?, ?>) cache
								.getNativeCache());
					}
					return null;
				}
			};
		}

		private CacheStatistics getConcurrentMapStatistics(ConcurrentHashMap<?, ?> map) {
			DefaultCacheStatistics stats = new DefaultCacheStatistics();
			stats.setSize((long) map.size());
			return stats;
		}
	}

	@Configuration
	@ConditionalOnClass(NoOpCacheManager.class)
	static class NoOpCacheStatisticsConfiguration {

		private static final CacheStatistics NO_OP_STATS = new DefaultCacheStatistics();

		@Bean
		public CacheStatisticsProvider noOpCacheStatisticsProvider() {
			return new CacheStatisticsProvider() {
				@Override
				public CacheStatistics getCacheStatistics(Cache cache,
						CacheManager cacheManager) {
					if (cacheManager instanceof NoOpCacheManager) {
						return NO_OP_STATS;
					}
					return null;
				}
			};
		}
	}

}
