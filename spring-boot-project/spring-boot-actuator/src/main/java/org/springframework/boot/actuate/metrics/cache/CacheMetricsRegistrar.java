/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Collection;
import java.util.Objects;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.util.ClassUtils;

/**
 * Register supported {@link Cache} to a {@link MeterRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CacheMetricsRegistrar {

	private final MeterRegistry registry;

	private final Collection<CacheMeterBinderProvider<?>> binderProviders;

	/**
	 * Creates a new registrar.
	 * @param registry the {@link MeterRegistry} to use
	 * @param binderProviders the {@link CacheMeterBinderProvider} instances that should
	 * be used to detect compatible caches
	 */
	public CacheMetricsRegistrar(MeterRegistry registry, Collection<CacheMeterBinderProvider<?>> binderProviders) {
		this.registry = registry;
		this.binderProviders = binderProviders;
	}

	/**
	 * Attempt to bind the specified {@link Cache} to the registry. Return {@code true} if
	 * the cache is supported and was bound to the registry, {@code false} otherwise.
	 * @param cache the cache to handle
	 * @param tags the tags to associate with the metrics of that cache
	 * @return {@code true} if the {@code cache} is supported and was registered
	 */
	public boolean bindCacheToRegistry(Cache cache, Tag... tags) {
		MeterBinder meterBinder = getMeterBinder(unwrapIfNecessary(cache), Tags.of(tags));
		if (meterBinder != null) {
			meterBinder.bindTo(this.registry);
			return true;
		}
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	private MeterBinder getMeterBinder(Cache cache, Tags tags) {
		Tags cacheTags = tags.and(getAdditionalTags(cache));
		return LambdaSafe.callbacks(CacheMeterBinderProvider.class, this.binderProviders, cache)
				.withLogger(CacheMetricsRegistrar.class)
				.invokeAnd((binderProvider) -> binderProvider.getMeterBinder(cache, cacheTags)).filter(Objects::nonNull)
				.findFirst().orElse(null);
	}

	/**
	 * Return additional {@link Tag tags} to be associated with the given {@link Cache}.
	 * @param cache the cache
	 * @return a list of additional tags to associate to that {@code cache}.
	 */
	protected Iterable<Tag> getAdditionalTags(Cache cache) {
		return Tags.of("name", cache.getName());
	}

	private Cache unwrapIfNecessary(Cache cache) {
		if (ClassUtils.isPresent("org.springframework.cache.transaction.TransactionAwareCacheDecorator",
				getClass().getClassLoader())) {
			return TransactionAwareCacheDecoratorHandler.unwrapIfNecessary(cache);
		}
		return cache;
	}

	private static class TransactionAwareCacheDecoratorHandler {

		private static Cache unwrapIfNecessary(Cache cache) {
			try {
				if (cache instanceof TransactionAwareCacheDecorator) {
					return ((TransactionAwareCacheDecorator) cache).getTargetCache();
				}
			}
			catch (NoClassDefFoundError ex) {
				// Ignore
			}
			return cache;
		}

	}

}
