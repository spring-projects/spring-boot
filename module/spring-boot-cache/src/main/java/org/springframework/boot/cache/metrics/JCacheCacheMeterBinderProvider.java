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

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;

import org.springframework.cache.jcache.JCacheCache;

/**
 * {@link CacheMeterBinderProvider} implementation for JCache.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class JCacheCacheMeterBinderProvider implements CacheMeterBinderProvider<JCacheCache> {

	private final boolean registerCacheRemovalsAsFunctionCounter;

	/**
	 * Creates a {@code JCacheCacheMeterBinderProvider} that registers cache removals as a
	 * {@link Gauge}.
	 */
	public JCacheCacheMeterBinderProvider() {
		this(false);
	}

	/**
	 * Creates a {@code JCacheCacheMeterBinderProvider} that registers cache removals with
	 * a meter type that depends on the value of
	 * {@code registerCacheRemovalsAsFunctionCounter}. When {@code false}, cache removals
	 * are registered as a {@link Gauge}. When {@code true}, cache removals are registered
	 * as a {@link FunctionCounter}.
	 * @param registerCacheRemovalsAsFunctionCounter whether to register removals as a
	 * gauge or a function counter
	 * @since 3.4.12
	 */
	public JCacheCacheMeterBinderProvider(boolean registerCacheRemovalsAsFunctionCounter) {
		this.registerCacheRemovalsAsFunctionCounter = registerCacheRemovalsAsFunctionCounter;
	}

	@Override
	public MeterBinder getMeterBinder(JCacheCache cache, Iterable<Tag> tags) {
		return new JCacheMetrics<>(cache.getNativeCache(), tags, this.registerCacheRemovalsAsFunctionCounter);
	}

}
