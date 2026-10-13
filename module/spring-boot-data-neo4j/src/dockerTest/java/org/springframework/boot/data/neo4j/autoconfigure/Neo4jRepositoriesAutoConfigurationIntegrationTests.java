/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.neo4j.domain.country.CountryRepository;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to ensure that the properties get read and applied during the auto-configuration.
 *
 * @author Michael J. Simons
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class Neo4jRepositoriesAutoConfigurationIntegrationTests {

	@Container
	static final Neo4jContainer neo4j = TestImage.forContainer(Neo4jContainer.class);

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", neo4j::getAdminPassword);
	}

	@Autowired
	private CountryRepository countryRepository;

	@Test
	void ensureRepositoryIsReady() {
		assertThat(this.countryRepository.count()).isZero();
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = CountryRepository.class)
	@ImportAutoConfiguration({ Neo4jAutoConfiguration.class, DataNeo4jAutoConfiguration.class,
			DataNeo4jRepositoriesAutoConfiguration.class })
	static class TestConfiguration {

	}

}
