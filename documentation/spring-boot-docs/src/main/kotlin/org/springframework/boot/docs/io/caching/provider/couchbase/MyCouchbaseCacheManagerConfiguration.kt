/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.caching.provider.couchbase

import org.springframework.boot.cache.autoconfigure.CouchbaseCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyCouchbaseCacheManagerConfiguration {

	@Bean
	fun myCouchbaseCacheManagerBuilderCustomizer(): CouchbaseCacheManagerBuilderCustomizer {
		return CouchbaseCacheManagerBuilderCustomizer { builder ->
			builder
				.withCacheConfiguration(
					"cache1", CouchbaseCacheConfiguration
						.defaultCacheConfig().entryExpiry(Duration.ofSeconds(10))
				)
				.withCacheConfiguration(
					"cache2", CouchbaseCacheConfiguration
						.defaultCacheConfig().entryExpiry(Duration.ofMinutes(1))
				)
		}
	}

}

