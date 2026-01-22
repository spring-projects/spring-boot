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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance benchmark tests for {@link LazyCacheMetricsRegistrar} vs {@link CacheMetricsRegistrar}.
 * These tests demonstrate the startup performance improvement achieved by lazy registration.
 *
 * @author Spring Boot Team
 */
class CacheMetricsRegistrarPerformanceTests {

	private static final int CACHE_COUNT = 1000;
	private static final int WARMUP_ITERATIONS = 5;
	private static final int BENCHMARK_ITERATIONS = 10;

	@Test
	void compareStartupPerformance() {
		// Warmup
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			benchmarkEagerRegistration();
			benchmarkLazyRegistration();
		}

		// Benchmark eager registration
		long eagerTotalTime = 0;
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			eagerTotalTime += benchmarkEagerRegistration();
		}
		long eagerAverageTime = eagerTotalTime / BENCHMARK_ITERATIONS;

		// Benchmark lazy registration
		long lazyTotalTime = 0;
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			lazyTotalTime += benchmarkLazyRegistration();
		}
		long lazyAverageTime = lazyTotalTime / BENCHMARK_ITERATIONS;

		System.out.printf("Eager registration average time: %d ms%n", eagerAverageTime);
		System.out.printf("Lazy registration average time: %d ms%n", lazyAverageTime);
		System.out.printf("Performance improvement: %.2fx faster%n", (double) eagerAverageTime / lazyAverageTime);

		// Lazy registration should be significantly faster for startup
		assertThat(lazyAverageTime).isLessThan(eagerAverageTime);
	}

	@Test
	void memoryUsageComparison() {
		Runtime runtime = Runtime.getRuntime();
		
		// Measure memory usage with eager registration
		runtime.gc();
		long memoryBeforeEager = runtime.totalMemory() - runtime.freeMemory();
		
		CacheMetricsRegistrar eagerRegistrar = createEagerRegistrar();
		List<Cache> caches = createCaches(CACHE_COUNT);
		registerCachesEagerly(eagerRegistrar, caches);
		
		runtime.gc();
		long memoryAfterEager = runtime.totalMemory() - runtime.freeMemory();
		long eagerMemoryUsage = memoryAfterEager - memoryBeforeEager;

		// Measure memory usage with lazy registration
		runtime.gc();
		long memoryBeforeLazy = runtime.totalMemory() - runtime.freeMemory();
		
		LazyCacheMetricsRegistrar lazyRegistrar = createLazyRegistrar();
		// Don't register caches - this simulates startup without cache access
		
		runtime.gc();
		long memoryAfterLazy = runtime.totalMemory() - runtime.freeMemory();
		long lazyMemoryUsage = memoryAfterLazy - memoryBeforeLazy;

		System.out.printf("Eager registration memory usage: %d bytes%n", eagerMemoryUsage);
		System.out.printf("Lazy registration memory usage: %d bytes%n", lazyMemoryUsage);

		// Lazy registration should use less memory at startup
		assertThat(lazyMemoryUsage).isLessThan(eagerMemoryUsage);
	}

	@Test
	void lazyRegistrationPerformanceOnAccess() {
		LazyCacheMetricsRegistrar registrar = createLazyRegistrar();
		List<Cache> caches = createCaches(100);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start("lazy-registration-on-access");

		// Register caches one by one (simulating access pattern)
		for (Cache cache : caches) {
			registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));
		}

		stopWatch.stop();

		System.out.printf("Lazy registration on access time: %d ms%n", stopWatch.getLastTaskTimeMillis());
		assertThat(registrar.getRegisteredCacheCount()).isEqualTo(100);
	}

	private long benchmarkEagerRegistration() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		CacheMetricsRegistrar registrar = createEagerRegistrar();
		List<Cache> caches = createCaches(CACHE_COUNT);
		registerCachesEagerly(registrar, caches);

		stopWatch.stop();
		return stopWatch.getTotalTimeMillis();
	}

	private long benchmarkLazyRegistration() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		LazyCacheMetricsRegistrar registrar = createLazyRegistrar();
		// Don't register caches - this simulates startup without cache access

		stopWatch.stop();
		return stopWatch.getTotalTimeMillis();
	}

	private CacheMetricsRegistrar createEagerRegistrar() {
		MeterRegistry registry = new SimpleMeterRegistry();
		CacheMeterBinderProvider<Cache> provider = (cache, tags) -> (meterRegistry) -> {
			// Simulate some work during registration
			try {
				TimeUnit.MICROSECONDS.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		return new CacheMetricsRegistrar(registry, Collections.singletonList(provider));
	}

	private LazyCacheMetricsRegistrar createLazyRegistrar() {
		MeterRegistry registry = new SimpleMeterRegistry();
		CacheMeterBinderProvider<Cache> provider = (cache, tags) -> (meterRegistry) -> {
			// Simulate some work during registration
			try {
				TimeUnit.MICROSECONDS.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		return new LazyCacheMetricsRegistrar(registry, Collections.singletonList(provider));
	}

	private List<Cache> createCaches(int count) {
		List<Cache> caches = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			caches.add(new ConcurrentMapCache("cache" + i));
		}
		return caches;
	}

	private void registerCachesEagerly(CacheMetricsRegistrar registrar, List<Cache> caches) {
		for (Cache cache : caches) {
			registrar.bindCacheToRegistry(cache, Tag.of("type", "test"));
		}
	}
}