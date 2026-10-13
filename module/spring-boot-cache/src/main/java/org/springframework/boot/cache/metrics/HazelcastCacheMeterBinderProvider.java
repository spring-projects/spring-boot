/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.metrics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.hazelcast.spring.cache.HazelcastCache;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.HazelcastCacheMetrics;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.cache.metrics.HazelcastCacheMeterBinderProvider.HazelcastCacheMeterBinderProviderRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link CacheMeterBinderProvider} implementation for Hazelcast.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
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
			Assert.state(nativeCacheAccessor != null, "'nativeCacheAccessor' must not be null");
			Object nativeCache = ReflectionUtils.invokeMethod(nativeCacheAccessor, cache);
			return HazelcastCacheMetrics.class.getConstructor(Object.class, Iterable.class)
				.newInstance(nativeCache, tags);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create MeterBinder for Hazelcast", ex);
		}
	}

	static class HazelcastCacheMeterBinderProviderRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
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
