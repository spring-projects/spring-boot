/*
 * Copyright 2012-2023 the original author or authors.
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

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokenManager;
import org.neo4j.driver.AuthTokenManagers;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Neo4jAutoConfiguration}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class Neo4jAutoConfigurationIntegrationTests {

	@Container
	private static final Neo4jContainer<?> neo4jServer = new Neo4jContainer<>(DockerImageNames.neo4j())
		.withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10));

	@SpringBootTest
	@Nested
	class DriverWithDefaultAuthToken {

		@DynamicPropertySource
		static void neo4jProperties(DynamicPropertyRegistry registry) {
			registry.add("spring.neo4j.uri", neo4jServer::getBoltUrl);
			registry.add("spring.neo4j.authentication.username", () -> "neo4j");
			registry.add("spring.neo4j.authentication.password", neo4jServer::getAdminPassword);
		}

		@Autowired
		private Driver driver;

		@Test
		void driverCanHandleRequest() {
			try (Session session = this.driver.session(); Transaction tx = session.beginTransaction()) {
				Result statementResult = tx.run("MATCH (n:Thing) RETURN n LIMIT 1");
				assertThat(statementResult.hasNext()).isFalse();
				tx.commit();
			}
		}

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(Neo4jAutoConfiguration.class)
		static class TestConfiguration {

		}

	}

	@SpringBootTest
	@Nested
	class DriverWithDynamicAuthToken {

		@DynamicPropertySource
		static void neo4jProperties(DynamicPropertyRegistry registry) {
			registry.add("spring.neo4j.uri", neo4jServer::getBoltUrl);
			registry.add("spring.neo4j.authentication.username", () -> "wrong");
			registry.add("spring.neo4j.authentication.password", () -> "alsowrong");
		}

		@Autowired
		private Driver driver;

		@Test
		void driverCanHandleRequest() {
			try (Session session = this.driver.session(); Transaction tx = session.beginTransaction()) {
				Result statementResult = tx.run("MATCH (n:Thing) RETURN n LIMIT 1");
				assertThat(statementResult.hasNext()).isFalse();
				tx.commit();
			}
		}

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(Neo4jAutoConfiguration.class)
		static class TestConfiguration {

			@Bean
			AuthTokenManager authTokenManager() {
				return AuthTokenManagers.bearer(() -> AuthTokens.basic("neo4j", neo4jServer.getAdminPassword())
					.expiringAt(System.currentTimeMillis() + 5_000));
			}

		}

	}

	@SpringBootTest
	@Nested
	class DriverWithCustomConnectionDetailsIgnoresAuthTokenManager {

		@DynamicPropertySource
		static void neo4jProperties(DynamicPropertyRegistry registry) {
			registry.add("spring.neo4j.uri", neo4jServer::getBoltUrl);
			registry.add("spring.neo4j.authentication.username", () -> "wrong");
			registry.add("spring.neo4j.authentication.password", () -> "alsowrong");
		}

		@Autowired
		private Driver driver;

		@Test
		void driverCanHandleRequest() {
			try (Session session = this.driver.session(); Transaction tx = session.beginTransaction()) {
				Result statementResult = tx.run("MATCH (n:Thing) RETURN n LIMIT 1");
				assertThat(statementResult.hasNext()).isFalse();
				tx.commit();
			}
		}

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(Neo4jAutoConfiguration.class)
		static class TestConfiguration {

			@Bean
			AuthTokenManager authTokenManager() {
				return AuthTokenManagers.bearer(() -> AuthTokens.basic("wrongagain", "stillwrong")
					.expiringAt(System.currentTimeMillis() + 5_000));
			}

			@Bean
			Neo4jConnectionDetails connectionDetails() {
				return new Neo4jConnectionDetails() {

					@Override
					public URI getUri() {
						return URI.create(neo4jServer.getBoltUrl());
					}

					@Override
					public AuthToken getAuthToken() {
						return AuthTokens.basic("neo4j", neo4jServer.getAdminPassword());
					}

				};
			}

		}

	}

}
