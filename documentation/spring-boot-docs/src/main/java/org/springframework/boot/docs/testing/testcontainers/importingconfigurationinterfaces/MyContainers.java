/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.testcontainers.importingconfigurationinterfaces;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.neo4j.Neo4jContainer;

interface MyContainers {

	@Container
	MongoDBContainer mongoContainer = new MongoDBContainer("mongo:5.0");

	@Container
	Neo4jContainer neo4jContainer = new Neo4jContainer("neo4j:5");

}
