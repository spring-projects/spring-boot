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

package org.springframework.boot.autoconfigure.cache.support;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * A mock {@link CachingProvider} that exposes a JSR-107 cache manager for testing
 * purposes.
 *
 * @author Stephane Nicoll
 */
public class MockCachingProvider implements CachingProvider {

	@Override
	@SuppressWarnings("rawtypes")
	public CacheManager getCacheManager(URI uri, ClassLoader classLoader,
			Properties properties) {
		CacheManager cacheManager = mock(CacheManager.class);
		given(cacheManager.getURI()).willReturn(uri);
		given(cacheManager.getClassLoader()).willReturn(classLoader);
		final Map<String, Cache> caches = new HashMap<>();
		given(cacheManager.getCacheNames()).willReturn(caches.keySet());
		given(cacheManager.getCache(anyString())).willAnswer((invocation) -> {
			String cacheName = invocation.getArgument(0);
			return caches.get(cacheName);
		});
		given(cacheManager.createCache(anyString(), any(Configuration.class)))
				.will((invocation) -> {
					String cacheName = invocation.getArgument(0);
					Cache cache = mock(Cache.class);
					given(cache.getName()).willReturn(cacheName);
					caches.put(cacheName, cache);
					return cache;
				});
		return cacheManager;
	}

	@Override
	public ClassLoader getDefaultClassLoader() {
		return mock(ClassLoader.class);
	}

	@Override
	public URI getDefaultURI() {
		return null;
	}

	@Override
	public Properties getDefaultProperties() {
		return new Properties();
	}

	@Override
	public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
		return getCacheManager(uri, classLoader, getDefaultProperties());
	}

	@Override
	public CacheManager getCacheManager() {
		return getCacheManager(getDefaultURI(), getDefaultClassLoader());
	}

	@Override
	public void close() {
	}

	@Override
	public void close(ClassLoader classLoader) {
	}

	@Override
	public void close(URI uri, ClassLoader classLoader) {
	}

	@Override
	public boolean isSupported(OptionalFeature optionalFeature) {
		return false;
	}

}
