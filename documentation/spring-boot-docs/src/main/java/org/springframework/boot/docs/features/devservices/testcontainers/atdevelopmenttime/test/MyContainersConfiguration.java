/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.test;

import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class MyContainersConfiguration {

	@Bean
	@ServiceConnection
	public Neo4jContainer neo4jContainer() {
		return new Neo4jContainer("neo4j:5");
	}

}
