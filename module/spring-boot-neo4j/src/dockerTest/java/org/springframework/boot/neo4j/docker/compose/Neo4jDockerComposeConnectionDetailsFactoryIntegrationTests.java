/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.docker.compose;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.neo4j.autoconfigure.Neo4jConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link Neo4jDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class Neo4jDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "neo4j-compose.yaml", image = TestImage.NEO4J)
	void runCreatesConnectionDetailsThatCanAccessNeo4j(Neo4jConnectionDetails connectionDetails) {
		assertConnectionDetailsWithPassword(connectionDetails, "absolutelysecret");
	}

	private void assertConnectionDetailsWithPassword(Neo4jConnectionDetails connectionDetails, String password) {
		assertThat(connectionDetails.getAuthToken()).isEqualTo(AuthTokens.basic("neo4j", password));
		try (Driver driver = GraphDatabase.driver(connectionDetails.getUri(), connectionDetails.getAuthToken())) {
			assertThatNoException().isThrownBy(driver::verifyConnectivity);
		}
	}

}
