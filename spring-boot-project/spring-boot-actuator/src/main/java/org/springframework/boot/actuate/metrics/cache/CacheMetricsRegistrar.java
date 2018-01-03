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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.core.ResolvableType;

/**
 * Register supported {@link Cache} to a {@link MeterRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CacheMetricsRegistrar {

	private final MeterRegistry registry;

	private final String metricName;

	private final Collection<CacheMeterBinderProvider<?>> binderProviders;

	/**
	 * Creates a new registrar.
	 * @param registry the {@link MeterRegistry} to use
	 * @param metricName the name of the metric
	 * @param binderProviders the {@link CacheMeterBinderProvider} instances that should
	 * be used to detect compatible caches
	 */
	public CacheMetricsRegistrar(MeterRegistry registry, String metricName,
			Collection<CacheMeterBinderProvider<?>> binderProviders) {
		this.registry = registry;
		this.metricName = metricName;
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
		List<Tag> allTags = new ArrayList<>(Arrays.asList(tags));
		MeterBinder meterBinder = getMeterBinder(cache, allTags);
		if (meterBinder != null) {
			meterBinder.bindTo(this.registry);
			return true;
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MeterBinder getMeterBinder(Cache cache, List<Tag> tags) {
		tags.addAll(getAdditionalTags(cache));
		for (CacheMeterBinderProvider<?> binderProvider : this.binderProviders) {
			Class<?> cacheType = ResolvableType
					.forClass(CacheMeterBinderProvider.class, binderProvider.getClass())
					.resolveGeneric();
			if (cacheType.isInstance(cache)) {
				try {
					MeterBinder meterBinder = ((CacheMeterBinderProvider) binderProvider)
							.getMeterBinder(cache, this.metricName, tags);
					if (meterBinder != null) {
						return meterBinder;
					}
				}
				catch (ClassCastException ex) {
					String msg = ex.getMessage();
					if (msg == null || msg.startsWith(cache.getClass().getName())) {
						// Possibly a lambda-defined listener which we could not resolve
						// the generic event type for
						Log logger = LogFactory.getLog(getClass());
						if (logger.isDebugEnabled()) {
							logger.debug(
									"Non-matching event type for CacheMeterBinderProvider: "
											+ binderProvider,
									ex);
						}
					}
					else {
						throw ex;
					}
				}
			}

		}
		return null;
	}

	/**
	 * Return additional {@link Tag tags} to be associated with the given {@link Cache}.
	 * @param cache the cache
	 * @return a list of additional tags to associate to that {@code cache}.
	 */
	protected List<Tag> getAdditionalTags(Cache cache) {
		return Tags.zip("name", cache.getName());
	}

}
