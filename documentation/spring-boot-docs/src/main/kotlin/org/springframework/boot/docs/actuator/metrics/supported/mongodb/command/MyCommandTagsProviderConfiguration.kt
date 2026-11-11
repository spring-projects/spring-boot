/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.command

import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyCommandTagsProviderConfiguration {

	@Bean
	fun customCommandTagsProvider(): MongoCommandTagsProvider? {
		return CustomCommandTagsProvider()
	}

}

