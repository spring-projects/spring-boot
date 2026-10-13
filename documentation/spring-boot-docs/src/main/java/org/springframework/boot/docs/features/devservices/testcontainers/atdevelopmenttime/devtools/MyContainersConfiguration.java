/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.devtools;

import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class MyContainersConfiguration {

	@Bean
	@RestartScope
	@ServiceConnection
	public MongoDBContainer mongoDbContainer() {
		return new MongoDBContainer("mongo:5.0");
	}

}
