/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.awaitility.Awaitility;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleFreeR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class OracleFreeR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "oracle-compose.yaml", image = TestImage.ORACLE_FREE)
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=freepdb1", "driver=oracle",
				"password=REDACTED", "user=app_user");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD))
			.isEqualTo("app_user_secret");
		Awaitility.await().atMost(Duration.ofMinutes(1)).ignoreExceptions().untilAsserted(() -> {
			String validationQuery = DatabaseDriver.ORACLE.getValidationQuery();
			assertThat(validationQuery).isNotNull();
			Object result = DatabaseClient.create(ConnectionFactories.get(connectionFactoryOptions))
				.sql(validationQuery)
				.map((row, metadata) -> row.get(0))
				.first()
				.block(Duration.ofSeconds(30));
			assertThat(result).isEqualTo("Hello");
		});
	}

}
