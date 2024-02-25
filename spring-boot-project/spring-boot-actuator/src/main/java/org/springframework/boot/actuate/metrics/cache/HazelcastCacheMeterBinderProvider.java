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

package org.springframework.boot.actuate.metrics.cache;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.metrics.cache.HazelcastCacheMeterBinderProvider.HazelcastCacheMeterBinderProviderRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link CacheMeterBinderProvider} implementation for Hazelcast.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ImportRuntimeHints(HazelcastCacheMeterBinderProviderRuntimeHints.class)
public class HazelcastCacheMeterBinderProvider implements CacheMeterBinderProvider<HazelcastCache> {

	/**
	 * Returns a MeterBinder for the given HazelcastCache and tags.
	 * @param cache the HazelcastCache to bind metrics for
	 * @param tags the tags to associate with the metrics
	 * @return a MeterBinder for the HazelcastCache and tags
	 */
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

	/**
	 * Creates a MeterBinder for Hazelcast 4 cache metrics.
	 * @param cache The HazelcastCache instance for which metrics are to be created.
	 * @param tags The tags to be associated with the metrics.
	 * @return The MeterBinder instance for Hazelcast 4 cache metrics.
	 * @throws IllegalStateException if failed to create the MeterBinder for Hazelcast.
	 */
	private MeterBinder createHazelcast4CacheMetrics(HazelcastCache cache, Iterable<Tag> tags) {
		try {
			Method nativeCacheAccessor = ReflectionUtils.findMethod(HazelcastCache.class, "getNativeCache");
			Object nativeCache = ReflectionUtils.invokeMethod(nativeCacheAccessor, cache);
			return HazelcastCacheMetrics.class.getConstructor(Object.class, Iterable.class)
				.newInstance(nativeCache, tags);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create MeterBinder for Hazelcast", ex);
		}
	}

	/**
	 * HazelcastCacheMeterBinderProviderRuntimeHints class.
	 */
	static class HazelcastCacheMeterBinderProviderRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers hints for the
		 * {@link HazelcastCacheMeterBinderProviderRuntimeHints#registerHints(RuntimeHints, ClassLoader)}
		 * method.
		 *
		 * This method is responsible for registering hints for the
		 * HazelcastCacheMeterBinderProviderRuntimeHints class. It takes two parameters:
		 * hints of type RuntimeHints and classLoader of type ClassLoader.
		 *
		 * The method first tries to find the 'getNativeCache' method using reflection and
		 * asserts that it is not null. If the method is found, it then tries to find the
		 * constructor of the HazelcastCacheMetrics class.
		 *
		 * The method then registers the 'getNativeCache' method and the constructor using
		 * the hints object.
		 *
		 * If the 'getNativeCache' method or the constructor is not found, an
		 * IllegalStateException is thrown.
		 * @param hints The RuntimeHints object used for registering hints.
		 * @param classLoader The ClassLoader object used for finding the 'getNativeCache'
		 * method.
		 * @throws IllegalStateException if the 'getNativeCache' method or the constructor
		 * is not found.
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			try {
				Method getNativeCacheMethod = ReflectionUtils.findMethod(HazelcastCache.class, "getNativeCache");
				Assert.state(getNativeCacheMethod != null, "Unable to find 'getNativeCache' method");
				Constructor<?> constructor = HazelcastCacheMetrics.class.getConstructor(Object.class, Iterable.class);
				hints.reflection()
					.registerMethod(getNativeCacheMethod, ExecutableMode.INVOKE)
					.registerConstructor(constructor, ExecutableMode.INVOKE);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException(ex);
			}

		}

	}

}
