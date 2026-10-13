/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import reactor.core.publisher.Mono;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClickHouseR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ClickHouseR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "clickhouse-compose.yaml", image = TestImage.CLICKHOUSE)
	void runCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		checkDatabaseAccess(connectionDetails);
	}

	private void assertConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=mydatabase", "driver=clickhouse",
				"password=REDACTED", "user=myuser");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
	}

	private void checkDatabaseAccess(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		ConnectionFactory connectionFactory = ConnectionFactories.get(connectionFactoryOptions);
		String sql = DatabaseDriver.CLICKHOUSE.getValidationQuery();
		Integer result = Mono.from(connectionFactory.create())
			.flatMapMany((connection) -> connection.createStatement(sql).execute())
			.flatMap((r) -> r.map((row, rowMetadata) -> row.get(0, Integer.class)))
			.blockFirst(Duration.ofSeconds(30));
		assertThat(result).isEqualTo(1);
	}

}
