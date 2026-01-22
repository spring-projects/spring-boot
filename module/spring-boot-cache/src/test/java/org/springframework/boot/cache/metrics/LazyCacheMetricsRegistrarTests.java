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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LazyCacheMetricsRegistrar}.
 *
 * @author Spring Boot Team
 */
class LazyCacheMetricsRegistrarTests {

	private MeterRegistry meterRegistry;

	private CacheMeterBinderProvider<Cache> binderProvider;

	private LazyCacheMetricsRegistrar registrar;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.meterRegistry = new SimpleMeterRegistry();
		this.binderProvider = mock(CacheMeterBinderProvider.class);
		this.registrar = new LazyCacheMetricsRegistrar(this.meterRegistry, Collections.singletonList(this.binderProvider));
	}

	@Test
	void bindCacheToRegistrySuccessfully() {
		Cache cache = new ConcurrentMapCache("test");
		given(this.binderProvider.getMeterBinder(eq(cache), any())).willReturn((registry) -> {});

		boolean result = this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));

		assertThat(result).isTrue();
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(1);
		assertThat(this.registrar.isCacheRegistered(cache, Tag.of("type", "test"))).isTrue();
	}

	@Test
	void bindCacheToRegistryOnlyOnce() {
		Cache cache = new ConcurrentMapCache("test");
		given(this.binderProvider.getMeterBinder(eq(cache), any())).willReturn((registry) -> {});

		// Bind the same cache multiple times
		this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));
		this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));
		this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));

		// Verify the binder provider is called only once
		verify(this.binderProvider, times(1)).getMeterBinder(eq(cache), any());
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(1);
	}

	@Test
	void bindDifferentCachesWithSameName() {
		Cache cache1 = new ConcurrentMapCache("test");
		Cache cache2 = new ConcurrentMapCache("test");
		given(this.binderProvider.getMeterBinder(any(Cache.class), any())).willReturn((registry) -> {});

		this.registrar.bindCacheToRegistry(cache1, Tag.of("type", "test"));
		this.registrar.bindCacheToRegistry(cache2, Tag.of("type", "test"));

		// Both caches should be registered as they are different instances
		verify(this.binderProvider, times(2)).getMeterBinder(any(Cache.class), any());
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(2);
	}

	@Test
	void bindCacheWithDifferentTags() {
		Cache cache = new ConcurrentMapCache("test");
		given(this.binderProvider.getMeterBinder(eq(cache), any())).willReturn((registry) -> {});

		this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test1"));
		this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test2"));

		// Same cache with different tags should be registered separately
		verify(this.binderProvider, times(2)).getMeterBinder(eq(cache), any());
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(2);
	}

	@Test
	void bindUnsupportedCache() {
		Cache cache = new ConcurrentMapCache("test");
		given(this.binderProvider.getMeterBinder(eq(cache), any())).willReturn(null);

		boolean result = this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));

		assertThat(result).isFalse();
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(0);
		assertThat(this.registrar.isCacheRegistered(cache, Tag.of("type", "test"))).isFalse();
	}

	@Test
	void threadSafetyTest() throws InterruptedException {
		Cache cache = new ConcurrentMapCache("test");
		AtomicInteger binderCallCount = new AtomicInteger(0);
		given(this.binderProvider.getMeterBinder(eq(cache), any())).willAnswer(invocation -> {
			binderCallCount.incrementAndGet();
			return (registry) -> {};
		});

		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// Submit multiple threads trying to register the same cache
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					this.registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(5, TimeUnit.SECONDS);
		executor.shutdown();

		// Verify that the cache was registered only once despite multiple threads
		assertThat(binderCallCount.get()).isEqualTo(1);
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(1);
	}

	@Test
	void generateCacheKeyUniqueness() {
		Cache cache1 = new ConcurrentMapCache("test");
		Cache cache2 = new ConcurrentMapCache("test");
		
		// Test that different cache instances with same name generate different keys
		this.registrar.bindCacheToRegistry(cache1, Tag.of("type", "test"));
		this.registrar.bindCacheToRegistry(cache2, Tag.of("type", "test"));
		
		assertThat(this.registrar.isCacheRegistered(cache1, Tag.of("type", "test"))).isTrue();
		assertThat(this.registrar.isCacheRegistered(cache2, Tag.of("type", "test"))).isTrue();
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(2);
	}

	@Test
	void isCacheRegisteredWithNonRegisteredCache() {
		Cache cache = new ConcurrentMapCache("test");
		
		assertThat(this.registrar.isCacheRegistered(cache, Tag.of("type", "test"))).isFalse();
	}

	@Test
	void getRegisteredCacheCountInitiallyZero() {
		assertThat(this.registrar.getRegisteredCacheCount()).isEqualTo(0);
	}
}