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

import com.hazelcast.core.Hazelcast;
import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.cache2k.Cache2kBuilder;
import org.cache2k.extra.micrometer.Cache2kCacheMetrics;
import org.cache2k.extra.spring.SpringCache2kCache;

import org.springframework.boot.actuate.metrics.cache.Cache2kCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CaffeineCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.HazelcastCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.JCacheCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.RedisCacheMeterBinderProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;

/**
 * Configure {@link CacheMeterBinderProvider} beans.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterBinder.class)
class CacheMeterBinderProvidersConfiguration {

	/**
     * Cache2kCacheMeterBinderProviderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Cache2kBuilder.class, SpringCache2kCache.class, Cache2kCacheMetrics.class })
	static class Cache2kCacheMeterBinderProviderConfiguration {

		/**
         * Returns a new instance of Cache2kCacheMeterBinderProvider.
         * 
         * @return a new instance of Cache2kCacheMeterBinderProvider
         */
        @Bean
		Cache2kCacheMeterBinderProvider cache2kCacheMeterBinderProvider() {
			return new Cache2kCacheMeterBinderProvider();
		}

	}

	/**
     * CaffeineCacheMeterBinderProviderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ CaffeineCache.class, com.github.benmanes.caffeine.cache.Cache.class })
	static class CaffeineCacheMeterBinderProviderConfiguration {

		/**
         * Returns a new instance of CaffeineCacheMeterBinderProvider.
         * 
         * @return the CaffeineCacheMeterBinderProvider instance
         */
        @Bean
		CaffeineCacheMeterBinderProvider caffeineCacheMeterBinderProvider() {
			return new CaffeineCacheMeterBinderProvider();
		}

	}

	/**
     * HazelcastCacheMeterBinderProviderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ HazelcastCache.class, Hazelcast.class })
	static class HazelcastCacheMeterBinderProviderConfiguration {

		/**
         * Returns a new instance of HazelcastCacheMeterBinderProvider.
         *
         * @return a new instance of HazelcastCacheMeterBinderProvider
         */
        @Bean
		HazelcastCacheMeterBinderProvider hazelcastCacheMeterBinderProvider() {
			return new HazelcastCacheMeterBinderProvider();
		}

	}

	/**
     * JCacheCacheMeterBinderProviderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JCacheCache.class, javax.cache.CacheManager.class })
	static class JCacheCacheMeterBinderProviderConfiguration {

		/**
         * Returns a new instance of JCacheCacheMeterBinderProvider.
         * 
         * @return a new instance of JCacheCacheMeterBinderProvider
         */
        @Bean
		JCacheCacheMeterBinderProvider jCacheCacheMeterBinderProvider() {
			return new JCacheCacheMeterBinderProvider();
		}

	}

	/**
     * RedisCacheMeterBinderProviderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RedisCache.class)
	static class RedisCacheMeterBinderProviderConfiguration {

		/**
         * Returns a RedisCacheMeterBinderProvider instance.
         *
         * @return a RedisCacheMeterBinderProvider instance
         */
        @Bean
		RedisCacheMeterBinderProvider redisCacheMeterBinderProvider() {
			return new RedisCacheMeterBinderProvider();
		}

	}

}
