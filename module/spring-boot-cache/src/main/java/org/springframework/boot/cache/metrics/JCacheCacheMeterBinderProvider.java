/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;

import org.springframework.cache.jcache.JCacheCache;

/**
 * {@link CacheMeterBinderProvider} implementation for JCache.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class JCacheCacheMeterBinderProvider implements CacheMeterBinderProvider<JCacheCache> {

	private final boolean registerCacheRemovalsAsFunctionCounter;

	/**
	 * Creates a {@code JCacheCacheMeterBinderProvider} that registers cache removals as a
	 * {@link Gauge}.
	 */
	public JCacheCacheMeterBinderProvider() {
		this(false);
	}

	/**
	 * Creates a {@code JCacheCacheMeterBinderProvider} that registers cache removals with
	 * a meter type that depends on the value of
	 * {@code registerCacheRemovalsAsFunctionCounter}. When {@code false}, cache removals
	 * are registered as a {@link Gauge}. When {@code true}, cache removals are registered
	 * as a {@link FunctionCounter}.
	 * @param registerCacheRemovalsAsFunctionCounter whether to register removals as a
	 * gauge or a function counter
	 * @since 3.4.12
	 */
	public JCacheCacheMeterBinderProvider(boolean registerCacheRemovalsAsFunctionCounter) {
		this.registerCacheRemovalsAsFunctionCounter = registerCacheRemovalsAsFunctionCounter;
	}

	@Override
	public MeterBinder getMeterBinder(JCacheCache cache, Iterable<Tag> tags) {
		return new JCacheMetrics<>(cache.getNativeCache(), tags, this.registerCacheRemovalsAsFunctionCounter);
	}

}
