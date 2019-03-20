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

import org.springframework.cache.CacheManager;

/**
 * Callback interface that can be implemented by beans wishing to customize the cache
 * manager before it is fully initialized, in particular to tune its configuration.
 *
 * @param <T> the type of the {@link CacheManager}
 * @author Stephane Nicoll
 * @since 1.3.3
 */
@FunctionalInterface
public interface CacheManagerCustomizer<T extends CacheManager> {

	/**
	 * Customize the cache manager.
	 * @param cacheManager the {@code CacheManager} to customize
	 */
	void customize(T cacheManager);

}
