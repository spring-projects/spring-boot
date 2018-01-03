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

import com.hazelcast.core.IMap;
import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.jcache.JCacheCache;

/**
 * Common {@link CacheMeterBinderProvider} implementations.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class CacheMeterBinderProviders {

	/**
	 * {@link CacheMeterBinderProvider} implementation for Caffeine.
	 */
	public static class CaffeineCacheMeterBinderProvider
			implements CacheMeterBinderProvider {

		@Override
		public MeterBinder getMeterBinder(Cache cache, String name, Iterable<Tag> tags) {
			if (cache instanceof CaffeineCache) {
				return new CaffeineCacheMetrics(
						((CaffeineCache) cache).getNativeCache(), tags, name);
			}
			return null;
		}
	}

	/**
	 * {@link CacheMeterBinderProvider} implementation for EhCache2.
	 */
	public static class EhCache2CacheMeterBinderProvider
			implements CacheMeterBinderProvider {

		@Override
		public MeterBinder getMeterBinder(Cache cache, String name, Iterable<Tag> tags) {
			if (cache instanceof EhCacheCache) {
				return new EhCache2Metrics(((EhCacheCache) cache).getNativeCache(),
						name, tags);
			}
			return null;
		}
	}

	/**
	 * {@link CacheMeterBinderProvider} implementation for Hazelcast.
	 */
	public static class HazelcastCacheMeterBinderProvider
			implements CacheMeterBinderProvider {

		@Override
		public MeterBinder getMeterBinder(Cache cache, String name, Iterable<Tag> tags) {
			if (cache instanceof HazelcastCache) {
				IMap<Object, Object> nativeCache = (IMap<Object, Object>) ((HazelcastCache) cache).getNativeCache();
				return new HazelcastCacheMetrics(nativeCache, name, tags);
			}
			return null;
		}
	}

	/**
	 * {@link CacheMeterBinderProvider} implementation for JCache.
	 */
	public static class JCacheCacheMeterBinderProvider
			implements CacheMeterBinderProvider {
		@Override
		public MeterBinder getMeterBinder(Cache cache, String name, Iterable<Tag> tags) {
			if (cache instanceof JCacheCache) {
				return new JCacheMetrics(((JCacheCache) cache).getNativeCache(),
						name, tags);
			}
			return null;
		}
	}

}
