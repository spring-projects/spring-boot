/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.cache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hazelcast.core.IMap;
import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProviders.CaffeineCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProviders.EhCache2CacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProviders.HazelcastCacheMeterBinderProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProviders.JCacheCacheMeterBinderProvider;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.jcache.JCacheCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CacheMeterBinderProviders}.
 *
 * @author Stephane Nicoll
 */
public class CacheMeterBinderProvidersTests {

	@Test
	public void caffeineCacheProvider() {
		CaffeineCache cache = new CaffeineCache("test", Caffeine.newBuilder().build());
		MeterBinder meterBinder = new CaffeineCacheMeterBinderProvider().getMeterBinder(
				cache, "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isInstanceOf(CaffeineCacheMetrics.class);
	}

	@Test
	public void caffeineCacheProviderWithUnsupportedCache() {
		MeterBinder meterBinder = new CaffeineCacheMeterBinderProvider().getMeterBinder(
				new ConcurrentMapCache("test"), "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isNull();
	}

	@Test
	public void ehCache2CacheProvider() {
		CacheManager cacheManager = new CacheManager(
				new Configuration().name("EhCacheCacheTests").defaultCache(
						new CacheConfiguration("default", 100)));
		try {
			Cache nativeCache = new Cache(
					new CacheConfiguration("test", 100));
			cacheManager.addCache(nativeCache);
			EhCacheCache cache = new EhCacheCache(nativeCache);
			MeterBinder meterBinder = new EhCache2CacheMeterBinderProvider().getMeterBinder(
					cache, "test", Collections.EMPTY_LIST);
			assertThat(meterBinder).isInstanceOf(EhCache2Metrics.class);
		}
		finally {
			cacheManager.shutdown();
		}
	}

	@Test
	public void ehCache2CacheProviderWithUnsupportedCache() {
		MeterBinder meterBinder = new EhCache2CacheMeterBinderProvider().getMeterBinder(
				new ConcurrentMapCache("test"), "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isNull();
	}

	@Test
	public void hazelcastCacheProvider() {
		IMap<Object, Object> nativeCache = mock(IMap.class);
		given(nativeCache.getName()).willReturn("test");
		HazelcastCache cache = new HazelcastCache(nativeCache);
		MeterBinder meterBinder = new HazelcastCacheMeterBinderProvider().getMeterBinder(
				cache, "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isInstanceOf(HazelcastCacheMetrics.class);
	}

	@Test
	public void hazelcastCacheProviderWithUnsupportedCache() {
		MeterBinder meterBinder = new HazelcastCacheMeterBinderProvider().getMeterBinder(
				new ConcurrentMapCache("test"), "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isNull();
	}

	@Test
	public void jCacheCacheProvider() throws URISyntaxException {
		javax.cache.CacheManager cacheManager = mock(javax.cache.CacheManager.class);
		given(cacheManager.getURI()).willReturn(new URI("/test"));
		javax.cache.Cache<Object, Object> nativeCache = mock(javax.cache.Cache.class);
		given(nativeCache.getCacheManager()).willReturn(cacheManager);
		given(nativeCache.getName()).willReturn("test");
		JCacheCache cache = new JCacheCache(nativeCache);
		MeterBinder meterBinder = new JCacheCacheMeterBinderProvider().getMeterBinder(
				cache, "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isInstanceOf(JCacheMetrics.class);
	}

	@Test
	public void jCacheCacheWithUnsupportedCache() {
		MeterBinder meterBinder = new JCacheCacheMeterBinderProvider().getMeterBinder(
				new ConcurrentMapCache("test"), "test", Collections.EMPTY_LIST);
		assertThat(meterBinder).isNull();
	}

}
