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

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Provide a {@link CacheStatistics} based on a {@link Cache}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public interface CacheStatisticsProvider {

	/**
	 * Return the current {@link CacheStatistics} snapshot for the specified {@link Cache}
	 * or {@code null} if the given cache could not be handled.
	 * @param cache the cache to handle
	 * @param cacheManager the {@link CacheManager} handling this cache
	 * @return the current cache statistics or {@code null}
	 */
	CacheStatistics getCacheStatistics(Cache cache, CacheManager cacheManager);

}
