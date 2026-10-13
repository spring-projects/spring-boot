/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.testcontainers.serviceconnections

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@Testcontainers
@SpringBootTest
class MyIntegrationTests {

	@Test
	fun myTest() {
		/**/ println()
	}

	companion object {

		@Container
		@ServiceConnection
		@JvmStatic
		val neo4j = Neo4jContainer("neo4j:5");

	}

}

