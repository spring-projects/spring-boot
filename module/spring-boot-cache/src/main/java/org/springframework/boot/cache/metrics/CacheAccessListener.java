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

import org.springframework.cache.Cache;

/**
 * Listener interface for cache access events. Implementations can be notified
 * when caches are accessed to perform lazy initialization of metrics or other
 * cache-related operations.
 *
 * @author Spring Boot Team
 * @since 4.0.0
 */
@FunctionalInterface
public interface CacheAccessListener {

	/**
	 * Called when a cache is accessed for the first time or when access patterns
	 * need to be tracked.
	 * @param cache the cache that was accessed
	 * @param cacheManagerName the name of the cache manager that owns this cache
	 */
	void onCacheAccess(Cache cache, String cacheManagerName);

}