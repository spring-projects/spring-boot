/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.health;

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
	private static final Neo4jContainer neo4jServer = TestImage.forContainer(Neo4jContainer.class);

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
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("edition", "community");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(Neo4jAutoConfiguration.class)
	@Import(Neo4jReactiveHealthIndicator.class)
	static class TestConfiguration {

	}

}
