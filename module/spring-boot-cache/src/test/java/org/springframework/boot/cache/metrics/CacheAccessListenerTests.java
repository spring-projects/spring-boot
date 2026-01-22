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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheAccessListener}.
 *
 * @author Spring Boot Team
 */
class CacheAccessListenerTests {

	@Test
	void functionalInterfaceImplementation() {
		AtomicInteger callCount = new AtomicInteger(0);
		Cache testCache = new ConcurrentMapCache("test");
		String cacheManagerName = "testManager";

		CacheAccessListener listener = (cache, managerName) -> {
			callCount.incrementAndGet();
			assertThat(cache).isEqualTo(testCache);
			assertThat(managerName).isEqualTo(cacheManagerName);
		};

		listener.onCacheAccess(testCache, cacheManagerName);

		assertThat(callCount.get()).isEqualTo(1);
	}

	@Test
	void lambdaImplementation() {
		AtomicInteger accessCount = new AtomicInteger(0);
		
		CacheAccessListener listener = (cache, cacheManagerName) -> {
			accessCount.incrementAndGet();
		};

		Cache cache1 = new ConcurrentMapCache("cache1");
		Cache cache2 = new ConcurrentMapCache("cache2");

		listener.onCacheAccess(cache1, "manager1");
		listener.onCacheAccess(cache2, "manager2");

		assertThat(accessCount.get()).isEqualTo(2);
	}

	@Test
	void methodReferenceImplementation() {
		TestCacheAccessHandler handler = new TestCacheAccessHandler();
		CacheAccessListener listener = handler::handleCacheAccess;

		Cache cache = new ConcurrentMapCache("test");
		listener.onCacheAccess(cache, "testManager");

		assertThat(handler.getAccessCount()).isEqualTo(1);
		assertThat(handler.getLastAccessedCache()).isEqualTo(cache);
		assertThat(handler.getLastCacheManagerName()).isEqualTo("testManager");
	}

	private static class TestCacheAccessHandler {
		private int accessCount = 0;
		private Cache lastAccessedCache;
		private String lastCacheManagerName;

		public void handleCacheAccess(Cache cache, String cacheManagerName) {
			this.accessCount++;
			this.lastAccessedCache = cache;
			this.lastCacheManagerName = cacheManagerName;
		}

		public int getAccessCount() {
			return accessCount;
		}

		public Cache getLastAccessedCache() {
			return lastAccessedCache;
		}

		public String getLastCacheManagerName() {
			return lastCacheManagerName;
		}
	}
}