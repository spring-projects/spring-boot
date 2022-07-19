/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.cache;

import java.lang.reflect.Method;
import java.util.Objects;

import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.metrics.cache.HazelcastCacheMeterBinderProvider.HazelcastCacheMeterBinderProviderRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.ReflectionUtils;

/**
 * {@link CacheMeterBinderProvider} implementation for Hazelcast.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ImportRuntimeHints(HazelcastCacheMeterBinderProviderRuntimeHints.class)
public class HazelcastCacheMeterBinderProvider implements CacheMeterBinderProvider<HazelcastCache> {

	@Override
	public MeterBinder getMeterBinder(HazelcastCache cache, Iterable<Tag> tags) {
		try {
			return new HazelcastCacheMetrics(cache.getNativeCache(), tags);
		}
		catch (NoSuchMethodError ex) {
			// Hazelcast 4
			return createHazelcast4CacheMetrics(cache, tags);
		}
	}

	private MeterBinder createHazelcast4CacheMetrics(HazelcastCache cache, Iterable<Tag> tags) {
		try {
			Method nativeCacheAccessor = ReflectionUtils.findMethod(HazelcastCache.class, "getNativeCache");
			Object nativeCache = ReflectionUtils.invokeMethod(nativeCacheAccessor, cache);
			return HazelcastCacheMetrics.class.getConstructor(Object.class, Iterable.class).newInstance(nativeCache,
					tags);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create MeterBinder for Hazelcast", ex);
		}
	}

	static class HazelcastCacheMeterBinderProviderRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			try {
				hints.reflection()
						.registerMethod(Objects
								.requireNonNull(ReflectionUtils.findMethod(HazelcastCache.class, "getNativeCache")))
						.registerConstructor(HazelcastCacheMetrics.class.getConstructor(Object.class, Iterable.class));
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException(ex);
			}

		}

	}

}
