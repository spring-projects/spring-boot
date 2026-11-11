/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcAdaptingLiquibaseConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class JdbcAdaptingLiquibaseConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "liquibase-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetails(LiquibaseConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
	}

}
