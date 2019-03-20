/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.cache.CaffeineCacheStatisticsProvider;
import org.springframework.boot.actuate.cache.ConcurrentMapCacheStatisticsProvider;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link CachePublicMetrics}
 *
 * @author Stephane Nicoll
 */
public class CachePublicMetricsTests {

	private Map<String, CacheManager> cacheManagers = new HashMap<String, CacheManager>();

	@Before
	public void setup() {
		this.cacheManagers.put("cacheManager",
				new ConcurrentMapCacheManager("foo", "bar"));
	}

	@Test
	public void cacheMetricsWithMatchingProvider() {
		CachePublicMetrics cpm = new CachePublicMetrics(this.cacheManagers,
				providers(new ConcurrentMapCacheStatisticsProvider()));
		Map<String, Number> metrics = metrics(cpm);
		assertThat(metrics).containsOnly(entry("cache.foo.size", 0L),
				entry("cache.bar.size", 0L));
	}

	@Test
	public void cacheMetricsWithNoMatchingProvider() {
		CachePublicMetrics cpm = new CachePublicMetrics(this.cacheManagers,
				providers(new CaffeineCacheStatisticsProvider()));
		Map<String, Number> metrics = metrics(cpm);
		assertThat(metrics).isEmpty();
	}

	@Test
	public void cacheMetricsWithMultipleCacheManagers() {
		this.cacheManagers.put("anotherCacheManager",
				new ConcurrentMapCacheManager("foo"));
		CachePublicMetrics cpm = new CachePublicMetrics(this.cacheManagers,
				providers(new ConcurrentMapCacheStatisticsProvider()));
		Map<String, Number> metrics = metrics(cpm);
		assertThat(metrics).containsOnly(entry("cache.cacheManager_foo.size", 0L),
				entry("cache.bar.size", 0L),
				entry("cache.anotherCacheManager_foo.size", 0L));
	}

	@Test
	public void cacheMetricsWithTransactionAwareCacheDecorator() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Collections.singletonList(
				new TransactionAwareCacheDecorator(new ConcurrentMapCache("foo"))));
		cacheManager.afterPropertiesSet();
		this.cacheManagers.put("cacheManager", cacheManager);
		CachePublicMetrics cpm = new CachePublicMetrics(this.cacheManagers,
				providers(new ConcurrentMapCacheStatisticsProvider()));
		Map<String, Number> metrics = metrics(cpm);
		assertThat(metrics).containsOnly(entry("cache.foo.size", 0L));
	}

	private Map<String, Number> metrics(CachePublicMetrics cpm) {
		Collection<Metric<?>> metrics = cpm.metrics();
		assertThat(metrics).isNotNull();
		Map<String, Number> result = new HashMap<String, Number>();
		for (Metric<?> metric : metrics) {
			result.put(metric.getName(), metric.getValue());
		}
		return result;
	}

	private Collection<CacheStatisticsProvider<?>> providers(
			CacheStatisticsProvider<?>... providers) {
		return Arrays.asList(providers);
	}

}
