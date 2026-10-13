/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.importingcontainerdeclarations;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

public interface MyContainers {

	@Container
	@ServiceConnection
	MongoDBContainer mongoContainer = new MongoDBContainer("mongo:5.0");

	@Container
	@ServiceConnection
	Neo4jContainer neo4jContainer = new Neo4jContainer("neo4j:5");

}
