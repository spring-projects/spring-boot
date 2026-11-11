/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.caching.provider.redis

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyRedisCacheManagerConfiguration {

	@Bean
	fun myRedisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer {
		return RedisCacheManagerBuilderCustomizer { builder ->
			builder
				.withCacheConfiguration(
					"cache1", RedisCacheConfiguration
						.defaultCacheConfig().entryTtl(Duration.ofSeconds(10))
				)
				.withCacheConfiguration(
					"cache2", RedisCacheConfiguration
						.defaultCacheConfig().entryTtl(Duration.ofMinutes(1))
				)
		}
	}

}

