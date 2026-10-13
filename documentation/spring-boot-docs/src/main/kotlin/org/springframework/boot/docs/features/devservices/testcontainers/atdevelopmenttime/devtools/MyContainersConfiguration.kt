/*
 * Copyright 2012-present the original author or authors.
 */
package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.devtools

import org.springframework.boot.devtools.restart.RestartScope
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mongodb.MongoDBContainer

@TestConfiguration(proxyBeanMethods = false)
class MyContainersConfiguration {

	@Bean
	@RestartScope
	@ServiceConnection
	fun mongoDbContainer(): MongoDBContainer {
		return MongoDBContainer("mongo:5.0")
	}

}

