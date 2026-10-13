/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import java.util.Collections;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.cache2k.extra.micrometer.Cache2kCacheMetrics;
import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Cache2kCacheMeterBinderProvider}.
 *
 * @author Stephane Nicoll
 */
class Cache2kCacheMeterBinderProviderTests {

	@Test
	void cache2kCacheProvider() {
		SpringCache2kCacheManager cacheManager = new SpringCache2kCacheManager()
			.addCaches((builder) -> builder.name("test"));
		MeterBinder meterBinder = new Cache2kCacheMeterBinderProvider().getMeterBinder(cacheManager.getCache("test"),
				Collections.emptyList());
		assertThat(meterBinder).isInstanceOf(Cache2kCacheMetrics.class);
		cacheManager.destroy();
	}

}
