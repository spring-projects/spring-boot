/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MariaDbR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MariaDbR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "mariadb-compose.yaml", image = TestImage.MARIADB)
	void runCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "mariadb-with-empty-root-password-compose.yaml", image = TestImage.MARIADB)
	void runWithEmptyRootPasswordCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=mydatabase", "driver=mariadb", "user=root");
		assertThat(connectionFactoryOptions.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
	}

	@DockerComposeTest(composeFile = "mariadb-with-random-root-password-compose.yaml", image = TestImage.MARIADB)
	void runWithRandomRootPasswordCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=mydatabase", "driver=mariadb",
				"password=REDACTED", "user=myuser");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
	}

}
