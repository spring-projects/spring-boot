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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.jcache.JCacheCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JCacheCacheMeterBinderProvider}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class JCacheCacheMeterBinderProviderTests {

	@Mock
	private javax.cache.Cache<Object, Object> nativeCache;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void jCacheCacheProvider(boolean recordRemovalsAsFunctionCounter) throws URISyntaxException {
		javax.cache.CacheManager cacheManager = mock(javax.cache.CacheManager.class);
		given(cacheManager.getURI()).willReturn(new URI("/test"));
		given(this.nativeCache.getCacheManager()).willReturn(cacheManager);
		given(this.nativeCache.getName()).willReturn("test");
		JCacheCache cache = new JCacheCache(this.nativeCache);
		MeterBinder meterBinder = new JCacheCacheMeterBinderProvider(recordRemovalsAsFunctionCounter)
			.getMeterBinder(cache, Collections.emptyList());
		assertThat(meterBinder).isInstanceOf(JCacheMetrics.class);
	}

}
