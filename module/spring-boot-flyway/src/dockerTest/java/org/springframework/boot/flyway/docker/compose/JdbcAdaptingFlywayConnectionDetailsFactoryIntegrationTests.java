/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcAdaptingFlywayConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class JdbcAdaptingFlywayConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "flyway-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetails(FlywayConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
	}

}
