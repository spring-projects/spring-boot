/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MariaDbJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MariaDbJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "mariadb-compose.yaml", image = TestImage.MARIADB)
	void runCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "mariadb-with-empty-root-password-compose.yaml", image = TestImage.MARIADB)
	void runWithEmptyRootPasswordCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEmpty();
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:mariadb://").endsWith("/mydatabase");
	}

	@DockerComposeTest(composeFile = "mariadb-with-random-root-password-compose.yaml", image = TestImage.MARIADB)
	void runWithRandomRootPasswordCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:mariadb://").endsWith("/mydatabase");
	}

}
