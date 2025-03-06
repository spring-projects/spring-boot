/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class CacheManagerCustomizersTests {

	@Test
	void customizeWithNullCustomizersShouldDoNothing() {
		new CacheManagerCustomizers(null).customize(mock(CacheManager.class));
	}

	@Test
	void customizeSimpleCacheManager() {
		CacheManagerCustomizers customizers = new CacheManagerCustomizers(
				Collections.singletonList(new CacheNamesCacheManagerCustomizer()));
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
		customizers.customize(cacheManager);
		assertThat(cacheManager.getCacheNames()).containsOnly("one", "two");
	}

	@Test
	void customizeShouldCheckGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestConcurrentMapCacheManagerCustomizer());
		CacheManagerCustomizers customizers = new CacheManagerCustomizers(list);
		customizers.customize(mock(CacheManager.class));
		assertThat(list.get(0).getCount()).isOne();
		assertThat(list.get(1).getCount()).isZero();
		customizers.customize(mock(ConcurrentMapCacheManager.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
		customizers.customize(mock(CaffeineCacheManager.class));
		assertThat(list.get(0).getCount()).isEqualTo(3);
		assertThat(list.get(1).getCount()).isOne();
	}

	static class CacheNamesCacheManagerCustomizer implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

		@Override
		public void customize(ConcurrentMapCacheManager cacheManager) {
			cacheManager.setCacheNames(Arrays.asList("one", "two"));
		}

	}

	static class TestCustomizer<T extends CacheManager> implements CacheManagerCustomizer<T> {

		private int count;

		@Override
		public void customize(T cacheManager) {
			this.count++;
		}

		int getCount() {
			return this.count;
		}

	}

	static class TestConcurrentMapCacheManagerCustomizer extends TestCustomizer<ConcurrentMapCacheManager> {

	}

}
