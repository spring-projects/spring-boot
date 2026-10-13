/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import java.util.Collections;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheMetricsRegistrar}.
 *
 * @author Stephane Nicoll
 */
class CacheMetricsRegistrarTests {

	private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@Test
	void bindToSupportedCache() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(this.meterRegistry,
				Collections.singleton(new CaffeineCacheMeterBinderProvider()));
		assertThat(
				registrar.bindCacheToRegistry(new CaffeineCache("test", Caffeine.newBuilder().recordStats().build())))
			.isTrue();
		assertThat(this.meterRegistry.get("cache.gets").tags("name", "test").meter()).isNotNull();
	}

	@Test
	void bindToSupportedCacheWrappedInTransactionProxy() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(this.meterRegistry,
				Collections.singleton(new CaffeineCacheMeterBinderProvider()));
		assertThat(registrar.bindCacheToRegistry(new TransactionAwareCacheDecorator(
				new CaffeineCache("test", Caffeine.newBuilder().recordStats().build()))))
			.isTrue();
		assertThat(this.meterRegistry.get("cache.gets").tags("name", "test").meter()).isNotNull();
	}

	@Test
	void bindToUnsupportedCache() {
		CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(this.meterRegistry, Collections.emptyList());
		assertThat(
				registrar.bindCacheToRegistry(new CaffeineCache("test", Caffeine.newBuilder().recordStats().build())))
			.isFalse();
		assertThat(this.meterRegistry.find("cache.gets").tags("name", "test").meter()).isNull();
	}

}
