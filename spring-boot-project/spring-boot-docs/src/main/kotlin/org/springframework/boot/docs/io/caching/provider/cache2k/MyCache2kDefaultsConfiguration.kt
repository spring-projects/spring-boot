package org.springframework.boot.docs.io.caching.provider.cache2k

import org.springframework.boot.autoconfigure.cache.Cache2kBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration(proxyBeanMethods = false)
class MyCache2kDefaultsConfiguration {

	@Bean
	fun myCache2kDefaultsCustomizer(): Cache2kBuilderCustomizer {
		return Cache2kBuilderCustomizer { builder ->
			builder.entryCapacity(200)
				.expireAfterWrite(5, TimeUnit.MINUTES)
		}
	}
}