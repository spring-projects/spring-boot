/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.supported.mongodb.connectionpool

import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyConnectionPoolTagsProviderConfiguration {

	@Bean
	fun customConnectionPoolTagsProvider(): MongoConnectionPoolTagsProvider {
		return CustomConnectionPoolTagsProvider()
	}

}

