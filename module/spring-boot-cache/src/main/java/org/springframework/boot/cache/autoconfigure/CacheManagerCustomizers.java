/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.cache.CacheManager;

/**
 * Invokes the available {@link CacheManagerCustomizer} instances in the context for a
 * given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class CacheManagerCustomizers {

	private final List<CacheManagerCustomizer<?>> customizers;

	public CacheManagerCustomizers(@Nullable List<? extends CacheManagerCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList();
	}

	/**
	 * Customize the specified {@link CacheManager}. Locates all
	 * {@link CacheManagerCustomizer} beans able to handle the specified instance and
	 * invoke {@link CacheManagerCustomizer#customize(CacheManager)} on them.
	 * @param <T> the type of cache manager
	 * @param cacheManager the cache manager to customize
	 * @return the cache manager
	 */
	@SuppressWarnings("unchecked")
	public <T extends CacheManager> T customize(T cacheManager) {
		LambdaSafe.callbacks(CacheManagerCustomizer.class, this.customizers, cacheManager)
			.withLogger(CacheManagerCustomizers.class)
			.invoke((customizer) -> customizer.customize(cacheManager));
		return cacheManager;
	}

}
