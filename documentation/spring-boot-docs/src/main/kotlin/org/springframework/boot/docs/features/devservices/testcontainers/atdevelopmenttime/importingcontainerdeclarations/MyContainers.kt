/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.importingcontainerdeclarations

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.neo4j.Neo4jContainer

interface MyContainers {

	companion object {

		@Container
		@ServiceConnection
		@JvmField
		val mongoContainer = MongoDBContainer("mongo:5.0")

		@Container
		@ServiceConnection
		@JvmField
		val neo4jContainer = Neo4jContainer("neo4j:5")

	}

}
