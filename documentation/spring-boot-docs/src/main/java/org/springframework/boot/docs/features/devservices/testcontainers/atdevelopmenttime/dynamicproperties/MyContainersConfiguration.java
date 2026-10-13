/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.dynamicproperties;

import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
public class MyContainersConfiguration {

	@Bean
	public MongoDBContainer mongoDbContainer() {
		return new MongoDBContainer("mongo:5.0");
	}

	@Bean
	public DynamicPropertyRegistrar mongoDbProperties(MongoDBContainer container) {
		return (properties) -> {
			properties.add("spring.mongodb.host", container::getHost);
			properties.add("spring.mongodb.port", container::getFirstMappedPort);
		};
	}

}
