/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.neo4j.testcontainers;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.neo4j.autoconfigure.Neo4jConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DeprecatedNeo4jContainerConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link Neo4jContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedNeo4jContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final Neo4jContainer<?> container = TestImage.container(Neo4jContainer.class);

	@Autowired(required = false)
	private Neo4jConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToContainer() {
		assertThat(this.connectionDetails).isNotNull();
		try (Driver driver = GraphDatabase.driver(this.connectionDetails.getUri(),
				this.connectionDetails.getAuthToken())) {
			driver.verifyConnectivity();
		}
	}

}
