/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cache.metrics;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import org.springframework.cache.Cache;

/**
 * Lazy implementation of {@link CacheMetricsRegistrar} that registers cache metrics
 * only when caches are first accessed, improving application startup performance.
 * 
 * <p>This implementation maintains a registry of already-bound caches to avoid
 * duplicate registrations and provides thread-safe lazy initialization.
 *
 * @author Spring Boot Team
 * @since 4.0.0
 */
public class LazyCacheMetricsRegistrar extends CacheMetricsRegistrar {

	private final ConcurrentMap<String, Boolean> registeredCaches = new ConcurrentHashMap<>();

	/**
	 * Creates a new lazy registrar.
	 * @param registry the {@link MeterRegistry} to use
	 * @param binderProviders the {@link CacheMeterBinderProvider} instances that should
	 * be used to detect compatible caches
	 */
	public LazyCacheMetricsRegistrar(MeterRegistry registry, Collection<CacheMeterBinderProvider<?>> binderProviders) {
		super(registry, binderProviders);
	}

	/**
	 * Lazily bind the specified {@link Cache} to the registry if not already registered.
	 * This method is thread-safe and ensures each cache is registered only once.
	 * @param cache the cache to handle
	 * @param tags the tags to associate with the metrics of that cache
	 * @return {@code true} if the {@code cache} is supported and was registered
	 */
	@Override
	public boolean bindCacheToRegistry(Cache cache, Tag... tags) {
		String cacheKey = generateCacheKey(cache, tags);
		return registeredCaches.computeIfAbsent(cacheKey, key -> super.bindCacheToRegistry(cache, tags));
	}

	/**
	 * Generate a unique key for cache registration tracking.
	 * @param cache the cache instance
	 * @param tags the associated tags
	 * @return unique cache key
	 */
	private String generateCacheKey(Cache cache, Tag... tags) {
		StringBuilder keyBuilder = new StringBuilder(cache.getName());
		keyBuilder.append('@').append(System.identityHashCode(cache));
		for (Tag tag : tags) {
			keyBuilder.append('|').append(tag.getKey()).append('=').append(tag.getValue());
		}
		return keyBuilder.toString();
	}

	/**
	 * Check if a cache is already registered.
	 * @param cache the cache to check
	 * @param tags the tags associated with the cache
	 * @return {@code true} if the cache is already registered
	 */
	public boolean isCacheRegistered(Cache cache, Tag... tags) {
		return registeredCaches.containsKey(generateCacheKey(cache, tags));
	}

	/**
	 * Get the number of currently registered caches.
	 * @return the count of registered caches
	 */
	public int getRegisteredCacheCount() {
		return registeredCaches.size();
	}
}