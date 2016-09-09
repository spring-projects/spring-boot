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

import net.sf.ehcache.statistics.StatisticsGateway;

import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCache;

/**
 * {@link CacheStatisticsProvider} implementation for EhCache.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class EhCacheStatisticsProvider implements CacheStatisticsProvider<EhCacheCache> {

	@Override
	public CacheStatistics getCacheStatistics(CacheManager cacheManager,
			EhCacheCache cache) {
		DefaultCacheStatistics statistics = new DefaultCacheStatistics();
		StatisticsGateway ehCacheStatistics = cache.getNativeCache().getStatistics();
		statistics.setSize(ehCacheStatistics.getSize());
		Double hitRatio = ehCacheStatistics.cacheHitRatio();
		if (!hitRatio.isNaN()) {
			// ratio is calculated 'racily' and can drift marginally above unity,
			// so we cap it here
			double sanitizedHitRatio = (hitRatio > 1 ? 1 : hitRatio);
			statistics.setHitRatio(sanitizedHitRatio);
			statistics.setMissRatio(1 - sanitizedHitRatio);
		}
		return statistics;
	}

}
