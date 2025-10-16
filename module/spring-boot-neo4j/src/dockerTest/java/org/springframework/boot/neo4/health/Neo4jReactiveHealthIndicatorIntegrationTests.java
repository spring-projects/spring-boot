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

package org.springframework.boot.neo4.health;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.neo4j.health.Neo4jReactiveHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Neo4jReactiveHealthIndicator}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class Neo4jReactiveHealthIndicatorIntegrationTests {

	// gh-33428

	@Container
	private static final Neo4jContainer neo4jServer = TestImage.container(Neo4jContainer.class);

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4jServer::getBoltUrl);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", neo4jServer::getAdminPassword);
	}

	@Autowired
	private Neo4jReactiveHealthIndicator healthIndicator;

	@Test
	void health() {
		Health health = this.healthIndicator.health(true).block(Duration.ofSeconds(20));
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("edition", "community");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(Neo4jAutoConfiguration.class)
	@Import(Neo4jReactiveHealthIndicator.class)
	static class TestConfiguration {

	}

}
