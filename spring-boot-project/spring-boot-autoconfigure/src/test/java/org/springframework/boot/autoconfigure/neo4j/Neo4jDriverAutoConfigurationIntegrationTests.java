/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(
		initializers = Neo4jDriverAutoConfigurationIntegrationTests.Neo4jContainerBasedTestPropertyProvider.class)
class Neo4jDriverAutoConfigurationIntegrationTests {

	@Container
	private static Neo4jContainer neo4jServer = new Neo4jContainer<>();

	private final Driver driver;

	@Autowired
	Neo4jDriverAutoConfigurationIntegrationTests(Driver driver) {
		this.driver = driver;
	}

	@Test
	void ensureDriverIsOpen() {

		try (Session session = this.driver.session()) {
			StatementResult statementResult = session.run("MATCH (n:Thing) RETURN n LIMIT 1");
			assertThat(statementResult.hasNext()).isFalse();
		}
	}

	static class Neo4jContainerBasedTestPropertyProvider
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of(Neo4jDriverProperties.PREFIX + ".uri = " + neo4jServer.getBoltUrl(),
					Neo4jDriverProperties.PREFIX + ".authentication.username = neo4j",
					Neo4jDriverProperties.PREFIX + ".authentication.password = " + neo4jServer.getAdminPassword())
					.applyTo(applicationContext.getEnvironment());
		}

	}

	@Configuration
	@ImportAutoConfiguration(Neo4jDriverAutoConfiguration.class)
	static class TestConfiguration {

	}

}
