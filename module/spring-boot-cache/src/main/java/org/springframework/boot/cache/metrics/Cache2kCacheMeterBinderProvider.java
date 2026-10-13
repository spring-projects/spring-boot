/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.cache2k.extra.micrometer.Cache2kCacheMetrics;
import org.cache2k.extra.spring.SpringCache2kCache;

/**
 * {@link CacheMeterBinderProvider} implementation for cache2k.
 *
 * @author Jens Wilke
 * @since 4.0.0
 */
public class Cache2kCacheMeterBinderProvider implements CacheMeterBinderProvider<SpringCache2kCache> {

	@Override
	public MeterBinder getMeterBinder(SpringCache2kCache cache, Iterable<Tag> tags) {
		return new Cache2kCacheMetrics(cache.getNativeCache(), tags);
	}

}
