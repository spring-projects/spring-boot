/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.List;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Caffeine cache configuration.
 *
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Caffeine.class, CaffeineCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional({ CacheCondition.class })
class CaffeineCacheConfiguration {

	/**
     * Creates a CaffeineCacheManager bean with the specified cache properties, customizers, caffeine, caffeineSpec, and cacheLoader.
     * 
     * @param cacheProperties the cache properties to be used
     * @param customizers the customizers to be applied to the cache manager
     * @param caffeine the Caffeine instance to be used for cache creation
     * @param caffeineSpec the CaffeineSpec to be used for cache creation
     * @param cacheLoader the CacheLoader to be used for cache creation
     * @return the created CaffeineCacheManager bean
     */
    @Bean
	CaffeineCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers customizers,
			ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
			ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
		CaffeineCacheManager cacheManager = createCacheManager(cacheProperties, caffeine, caffeineSpec, cacheLoader);
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheManager.setCacheNames(cacheNames);
		}
		return customizers.customize(cacheManager);
	}

	/**
     * Creates a CaffeineCacheManager with the specified cache properties, caffeine provider, caffeine spec provider,
     * and cache loader provider.
     *
     * @param cacheProperties the cache properties to be used by the cache manager
     * @param caffeine the provider for the Caffeine instance to be used by the cache manager
     * @param caffeineSpec the provider for the CaffeineSpec to be used by the cache manager
     * @param cacheLoader the provider for the CacheLoader to be used by the cache manager
     * @return the created CaffeineCacheManager
     */
    private CaffeineCacheManager createCacheManager(CacheProperties cacheProperties,
			ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
			ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		setCacheBuilder(cacheProperties, caffeineSpec.getIfAvailable(), caffeine.getIfAvailable(), cacheManager);
		cacheLoader.ifAvailable(cacheManager::setCacheLoader);
		return cacheManager;
	}

	/**
     * Sets the cache builder for the Caffeine cache manager.
     * 
     * @param cacheProperties the cache properties
     * @param caffeineSpec the Caffeine specification
     * @param caffeine the Caffeine cache builder
     * @param cacheManager the Caffeine cache manager
     */
    private void setCacheBuilder(CacheProperties cacheProperties, CaffeineSpec caffeineSpec,
			Caffeine<Object, Object> caffeine, CaffeineCacheManager cacheManager) {
		String specification = cacheProperties.getCaffeine().getSpec();
		if (StringUtils.hasText(specification)) {
			cacheManager.setCacheSpecification(specification);
		}
		else if (caffeineSpec != null) {
			cacheManager.setCaffeineSpec(caffeineSpec);
		}
		else if (caffeine != null) {
			cacheManager.setCaffeine(caffeine);
		}
	}

}
