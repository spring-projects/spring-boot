/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import javax.cache.CacheManager;

/**
 * Callback interface that can be implemented by beans wishing to customize the cache
 * manager before it is used, in particular to create additional caches.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface JCacheManagerCustomizer {

	/**
	 * Customize the cache manager.
	 * @param cacheManager the {@code javax.cache.CacheManager} to customize
	 */
	void customize(CacheManager cacheManager);

}
