/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;

/**
 * {@link CacheMeterBinderProvider} implementation for Hazelcast.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class HazelcastCacheMeterBinderProvider implements CacheMeterBinderProvider<HazelcastCache> {

	@Override
	public MeterBinder getMeterBinder(HazelcastCache cache, Iterable<Tag> tags) {
		return new HazelcastCacheMetrics(cache.getNativeCache(), tags);
	}

}
