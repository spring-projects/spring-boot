/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import org.springframework.cache.CacheManager;

/**
 * Callback interface that can be implemented by beans wishing to customize the cache
 * manager before it is fully initialized, in particular to tune its configuration.
 *
 * @param <T> the type of the {@link CacheManager}
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface CacheManagerCustomizer<T extends CacheManager> {

	/**
	 * Customize the cache manager.
	 * @param cacheManager the {@code CacheManager} to customize
	 */
	void customize(T cacheManager);

}
