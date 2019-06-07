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

package org.springframework.boot.actuate.cache;

import java.util.Collection;
import java.util.Collections;

/**
 * Exception thrown when multiple caches exist with the same name.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class NonUniqueCacheException extends RuntimeException {

	private final String cacheName;

	private final Collection<String> cacheManagerNames;

	public NonUniqueCacheException(String cacheName, Collection<String> cacheManagerNames) {
		super(String.format("Multiple caches named %s found, specify the 'cacheManager' " + "to use: %s", cacheName,
				cacheManagerNames));
		this.cacheName = cacheName;
		this.cacheManagerNames = Collections.unmodifiableCollection(cacheManagerNames);
	}

	public String getCacheName() {
		return this.cacheName;
	}

	public Collection<String> getCacheManagerNames() {
		return this.cacheManagerNames;
	}

}
