/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import javax.cache.CacheManager;

/**
 * Callback interface that can be implemented by beans wishing to customize the cache
 * manager before it is used, in particular to create additional caches.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@FunctionalInterface
public interface JCacheManagerCustomizer {

	/**
	 * Customize the cache manager.
	 * @param cacheManager the {@code javax.cache.CacheManager} to customize
	 */
	void customize(CacheManager cacheManager);

}
