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

import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCache;

import com.google.common.cache.CacheStats;

/**
 * {@link CacheStatisticsProvider} implementation for {@link GuavaCache}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class GuavaCacheStatisticsProvider implements CacheStatisticsProvider<GuavaCache> {

	@Override
	public CacheStatistics getCacheStatistics(CacheManager cacheManager, GuavaCache cache) {
		DefaultCacheStatistics statistics = new DefaultCacheStatistics();
		statistics.setSize(cache.getNativeCache().size());
		CacheStats guavaStats = cache.getNativeCache().stats();
		if (guavaStats.requestCount() > 0) {
			statistics.setHitRatio(guavaStats.hitRate());
			statistics.setMissRatio(guavaStats.missRate());
		}
		return statistics;
	}

}
