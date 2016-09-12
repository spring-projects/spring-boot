/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.util.HashSet;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.support.GemfireCacheManager;

/**
 * GemFire cache configuration.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.data.gemfire.support.GemfireCacheManager
 * @since 1.5.0
 */
@Configuration
@ConditionalOnBean({ CacheFactoryBean.class, Cache.class,
	ClientCacheFactoryBean.class, ClientCache.class })
@ConditionalOnClass(GemfireCacheManager.class)
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
class GeodeCacheConfiguration {

	private final CacheProperties cacheProperties;

	private final CacheManagerCustomizers cacheManagerCustomizers;

	GeodeCacheConfiguration(CacheProperties cacheProperties,
			CacheManagerCustomizers cacheManagerCustomizers) {

		this.cacheProperties = cacheProperties;
		this.cacheManagerCustomizers = cacheManagerCustomizers;
	}

	@Bean
	public GemfireCacheManager cacheManager(GemFireCache gemfireCache) {
		GemfireCacheManager cacheManager = new GemfireCacheManager();

		cacheManager.setCache(gemfireCache);
		cacheManager.setCacheNames(new HashSet<String>(this.cacheProperties.getCacheNames()));

		return this.cacheManagerCustomizers.customize(cacheManager);
	}
}
