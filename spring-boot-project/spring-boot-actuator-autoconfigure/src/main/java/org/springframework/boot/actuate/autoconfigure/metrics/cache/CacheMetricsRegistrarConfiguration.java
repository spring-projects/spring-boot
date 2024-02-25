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

package org.springframework.boot.actuate.autoconfigure.metrics.cache;

import java.util.Collection;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configure a {@link CacheMetricsRegistrar} and register all available {@link Cache
 * caches}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({ CacheMeterBinderProvider.class, MeterRegistry.class })
class CacheMetricsRegistrarConfiguration {

	private static final String CACHE_MANAGER_SUFFIX = "cacheManager";

	private final MeterRegistry registry;

	private final CacheMetricsRegistrar cacheMetricsRegistrar;

	private final Map<String, CacheManager> cacheManagers;

	/**
     * Constructs a new CacheMetricsRegistrarConfiguration with the specified parameters.
     * 
     * @param registry the MeterRegistry to register cache metrics with
     * @param binderProviders the collection of CacheMeterBinderProviders to provide binders for cache metrics
     * @param cacheManagers the map of cache managers to bind caches to
     */
    CacheMetricsRegistrarConfiguration(MeterRegistry registry, Collection<CacheMeterBinderProvider<?>> binderProviders,
			Map<String, CacheManager> cacheManagers) {
		this.registry = registry;
		this.cacheManagers = cacheManagers;
		this.cacheMetricsRegistrar = new CacheMetricsRegistrar(this.registry, binderProviders);
		bindCachesToRegistry();
	}

	/**
     * Returns the CacheMetricsRegistrar instance.
     *
     * @return the CacheMetricsRegistrar instance
     */
    @Bean
	CacheMetricsRegistrar cacheMetricsRegistrar() {
		return this.cacheMetricsRegistrar;
	}

	/**
     * Binds the cache managers to the registry.
     * 
     * This method iterates over the list of cache managers and binds each cache manager to the registry.
     * 
     * @see CacheMetricsRegistrarConfiguration#bindCacheManagerToRegistry(CacheManager)
     */
    private void bindCachesToRegistry() {
		this.cacheManagers.forEach(this::bindCacheManagerToRegistry);
	}

	/**
     * Binds the given CacheManager to the registry with the specified bean name.
     * 
     * @param beanName the name of the bean to bind the CacheManager to
     * @param cacheManager the CacheManager to bind to the registry
     */
    private void bindCacheManagerToRegistry(String beanName, CacheManager cacheManager) {
		cacheManager.getCacheNames()
			.forEach((cacheName) -> bindCacheToRegistry(beanName, cacheManager.getCache(cacheName)));
	}

	/**
     * Binds the specified cache to the registry with the given bean name and cache manager tag.
     * 
     * @param beanName the name of the cache bean
     * @param cache the cache to be bound to the registry
     * @throws IllegalArgumentException if the cache or cache manager tag is null
     */
    private void bindCacheToRegistry(String beanName, Cache cache) {
		Tag cacheManagerTag = Tag.of("cache.manager", getCacheManagerName(beanName));
		this.cacheMetricsRegistrar.bindCacheToRegistry(cache, cacheManagerTag);
	}

	/**
	 * Get the name of a {@link CacheManager} based on its {@code beanName}.
	 * @param beanName the name of the {@link CacheManager} bean
	 * @return a name for the given cache manager
	 */
	private String getCacheManagerName(String beanName) {
		if (beanName.length() > CACHE_MANAGER_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, CACHE_MANAGER_SUFFIX)) {
			return beanName.substring(0, beanName.length() - CACHE_MANAGER_SUFFIX.length());
		}
		return beanName;
	}

}
