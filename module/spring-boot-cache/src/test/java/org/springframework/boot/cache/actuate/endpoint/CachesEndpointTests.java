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

package org.springframework.boot.cache.actuate.endpoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cache.actuate.endpoint.CachesEndpoint.CacheDescriptor;
import org.springframework.boot.cache.actuate.endpoint.CachesEndpoint.CacheEntryDescriptor;
import org.springframework.boot.cache.actuate.endpoint.CachesEndpoint.CacheManagerDescriptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link CachesEndpoint}.
 *
 * @author Stephane Nicoll
 */
class CachesEndpointTests {

	@Test
	void allCachesWithSingleCacheManager() {
		CachesEndpoint endpoint = new CachesEndpoint(
				Collections.singletonMap("test", new ConcurrentMapCacheManager("a", "b")));
		Map<String, CacheManagerDescriptor> allDescriptors = endpoint.caches().getCacheManagers();
		assertThat(allDescriptors).containsOnlyKeys("test");
		CacheManagerDescriptor descriptors = allDescriptors.get("test");
		assertThat(descriptors).isNotNull();
		assertThat(descriptors.getCaches()).containsOnlyKeys("a", "b");
		CacheDescriptor a = descriptors.getCaches().get("a");
		assertThat(a).isNotNull();
		assertThat(a.getTarget()).isEqualTo(ConcurrentHashMap.class.getName());
		CacheDescriptor b = descriptors.getCaches().get("b");
		assertThat(b).isNotNull();
		assertThat(b.getTarget()).isEqualTo(ConcurrentHashMap.class.getName());
	}

	@Test
	void allCachesWithSeveralCacheManagers() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		cacheManagers.put("test", new ConcurrentMapCacheManager("a", "b"));
		cacheManagers.put("another", new ConcurrentMapCacheManager("a", "c"));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		Map<String, CacheManagerDescriptor> allDescriptors = endpoint.caches().getCacheManagers();
		assertThat(allDescriptors).containsOnlyKeys("test", "another");
		CacheManagerDescriptor test = allDescriptors.get("test");
		assertThat(test).isNotNull();
		assertThat(test.getCaches()).containsOnlyKeys("a", "b");
		CacheManagerDescriptor another = allDescriptors.get("another");
		assertThat(another).isNotNull();
		assertThat(another.getCaches()).containsOnlyKeys("a", "c");
	}

	@Test
	void namedCacheWithSingleCacheManager() {
		CachesEndpoint endpoint = new CachesEndpoint(
				Collections.singletonMap("test", new ConcurrentMapCacheManager("b", "a")));
		CacheEntryDescriptor entry = endpoint.cache("a", null);
		assertThat(entry).isNotNull();
		assertThat(entry.getCacheManager()).isEqualTo("test");
		assertThat(entry.getName()).isEqualTo("a");
		assertThat(entry.getTarget()).isEqualTo(ConcurrentHashMap.class.getName());
	}

	@Test
	void namedCacheWithSeveralCacheManagers() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		cacheManagers.put("test", new ConcurrentMapCacheManager("b", "dupe-cache"));
		cacheManagers.put("another", new ConcurrentMapCacheManager("c", "dupe-cache"));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		assertThatExceptionOfType(NonUniqueCacheException.class).isThrownBy(() -> endpoint.cache("dupe-cache", null))
			.withMessageContaining("dupe-cache")
			.withMessageContaining("test")
			.withMessageContaining("another");
	}

	@Test
	void namedCacheWithUnknownCache() {
		CachesEndpoint endpoint = new CachesEndpoint(
				Collections.singletonMap("test", new ConcurrentMapCacheManager("b", "a")));
		CacheEntryDescriptor entry = endpoint.cache("unknown", null);
		assertThat(entry).isNull();
	}

	@Test
	void namedCacheWithWrongCacheManager() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		cacheManagers.put("test", new ConcurrentMapCacheManager("b", "a"));
		cacheManagers.put("another", new ConcurrentMapCacheManager("c", "a"));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		CacheEntryDescriptor entry = endpoint.cache("c", "test");
		assertThat(entry).isNull();
	}

	@Test
	void namedCacheWithSeveralCacheManagersWithCacheManagerFilter() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		cacheManagers.put("test", new ConcurrentMapCacheManager("b", "a"));
		cacheManagers.put("another", new ConcurrentMapCacheManager("c", "a"));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		CacheEntryDescriptor entry = endpoint.cache("a", "test");
		assertThat(entry).isNotNull();
		assertThat(entry.getCacheManager()).isEqualTo("test");
		assertThat(entry.getName()).isEqualTo("a");
	}

	@Test
	void clearAllCaches() {
		Cache a = mockCache("a");
		Cache b = mockCache("b");
		CachesEndpoint endpoint = new CachesEndpoint(Collections.singletonMap("test", cacheManager(a, b)));
		endpoint.clearCaches();
		then(a).should().clear();
		then(b).should().clear();
	}

	@Test
	void clearCache() {
		Cache a = mockCache("a");
		Cache b = mockCache("b");
		CachesEndpoint endpoint = new CachesEndpoint(Collections.singletonMap("test", cacheManager(a, b)));
		assertThat(endpoint.clearCache("a", null)).isTrue();
		then(a).should().clear();
		then(b).should(never()).clear();
	}

	@Test
	void clearCacheWithSeveralCacheManagers() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		cacheManagers.put("test", cacheManager(mockCache("dupe-cache"), mockCache("b")));
		cacheManagers.put("another", cacheManager(mockCache("dupe-cache")));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		assertThatExceptionOfType(NonUniqueCacheException.class)
			.isThrownBy(() -> endpoint.clearCache("dupe-cache", null))
			.withMessageContaining("dupe-cache")
			.withMessageContaining("test")
			.withMessageContaining("another");
	}

	@Test
	void clearCacheWithSeveralCacheManagersWithCacheManagerFilter() {
		Map<String, CacheManager> cacheManagers = new LinkedHashMap<>();
		Cache a = mockCache("a");
		Cache b = mockCache("b");
		cacheManagers.put("test", cacheManager(a, b));
		Cache anotherA = mockCache("a");
		cacheManagers.put("another", cacheManager(anotherA));
		CachesEndpoint endpoint = new CachesEndpoint(cacheManagers);
		assertThat(endpoint.clearCache("a", "another")).isTrue();
		then(a).should(never()).clear();
		then(anotherA).should().clear();
		then(b).should(never()).clear();
	}

	@Test
	void clearCacheWithUnknownCache() {
		Cache a = mockCache("a");
		CachesEndpoint endpoint = new CachesEndpoint(Collections.singletonMap("test", cacheManager(a)));
		assertThat(endpoint.clearCache("unknown", null)).isFalse();
		then(a).should(never()).clear();
	}

	@Test
	void clearCacheWithUnknownCacheManager() {
		Cache a = mockCache("a");
		CachesEndpoint endpoint = new CachesEndpoint(Collections.singletonMap("test", cacheManager(a)));
		assertThat(endpoint.clearCache("a", "unknown")).isFalse();
		then(a).should(never()).clear();
	}

	private CacheManager cacheManager(Cache... caches) {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(caches));
		cacheManager.afterPropertiesSet();
		return cacheManager;
	}

	private Cache mockCache(String name) {
		Cache cache = mock(Cache.class);
		given(cache.getName()).willReturn(name);
		given(cache.getNativeCache()).willReturn(new Object());
		return cache;
	}

}
