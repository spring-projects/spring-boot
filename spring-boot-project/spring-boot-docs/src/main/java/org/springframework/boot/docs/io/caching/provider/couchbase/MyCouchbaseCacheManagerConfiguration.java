/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.io.caching.provider.couchbase;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.CouchbaseCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration;

/**
 * MyCouchbaseCacheManagerConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyCouchbaseCacheManagerConfiguration {

	/**
     * Customizes the CouchbaseCacheManagerBuilder by adding cache configurations for cache1 and cache2.
     * 
     * @return the CouchbaseCacheManagerBuilderCustomizer
     */
    @Bean
	public CouchbaseCacheManagerBuilderCustomizer myCouchbaseCacheManagerBuilderCustomizer() {
		// @formatter:off
		return (builder) -> builder
				.withCacheConfiguration("cache1", CouchbaseCacheConfiguration
						.defaultCacheConfig().entryExpiry(Duration.ofSeconds(10)))
				.withCacheConfiguration("cache2", CouchbaseCacheConfiguration
						.defaultCacheConfig().entryExpiry(Duration.ofMinutes(1)));
		// @formatter:on

	}

}
