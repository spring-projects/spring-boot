/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.data.redis.cache.RedisCache;

/**
 * {@link CacheMeterBinderProvider} implementation for Redis.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class RedisCacheMeterBinderProvider implements CacheMeterBinderProvider<RedisCache> {

	@Override
	public MeterBinder getMeterBinder(RedisCache cache, Iterable<Tag> tags) {
		return new RedisCacheMetrics(cache, tags);
	}

}
