/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A {@link CacheStatisticsProvider} that returns the first {@link CacheStatistics} that
 * can be retrieved by one of its delegates.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class CacheStatisticsProviders implements CacheStatisticsProvider {

	private final List<CacheStatisticsProvider> providers;

	/**
	 * Create a {@link CacheStatisticsProviders} instance with the collection of delegates
	 * to use.
	 * @param providers the cache statistics providers
	 */
	public CacheStatisticsProviders(
			Collection<? extends CacheStatisticsProvider> providers) {
		this.providers = (providers == null ? Collections
				.<CacheStatisticsProvider> emptyList()
				: new ArrayList<CacheStatisticsProvider>(providers));
	}

	@Override
	public CacheStatistics getCacheStatistics(Cache cache, CacheManager cacheManager) {
		for (CacheStatisticsProvider provider : this.providers) {
			CacheStatistics cacheStatistics = provider.getCacheStatistics(cache,
					cacheManager);
			if (cacheStatistics != null) {
				return cacheStatistics;
			}
		}
		return null;
	}

}
