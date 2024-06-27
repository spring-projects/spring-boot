/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docker.compose.service.connection.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import org.springframework.boot.autoconfigure.neo4j.Neo4jConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
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
		assertConnectionDetailsWithPassword(connectionDetails, "secret");
	}

	@DockerComposeTest(composeFile = "neo4j-bitnami-compose.yaml", image = TestImage.BITNAMI_NEO4J)
	void runWithBitnamiImageCreatesConnectionDetailsThatCanAccessNeo4j(Neo4jConnectionDetails connectionDetails) {
		assertConnectionDetailsWithPassword(connectionDetails, "bitnami2");
	}

	private void assertConnectionDetailsWithPassword(Neo4jConnectionDetails connectionDetails, String password) {
		assertThat(connectionDetails.getAuthToken()).isEqualTo(AuthTokens.basic("neo4j", password));
		try (Driver driver = GraphDatabase.driver(connectionDetails.getUri(), connectionDetails.getAuthToken())) {
			assertThatNoException().isThrownBy(driver::verifyConnectivity);
		}
	}

}
