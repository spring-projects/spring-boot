/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.postgres;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PostgresR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "postgres-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-with-trust-host-auth-method-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetailsThatCanAccessDatabaseWhenHostAuthMethodIsTrust(
			R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("myuser");
		assertThat(connectionFactoryOptions.getValue(ConnectionFactoryOptions.PASSWORD)).isNull();
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.DATABASE))
			.isEqualTo("mydatabase");
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-bitnami-compose.yaml", image = TestImage.BITNAMI_POSTGRESQL)
	void runWithBitnamiImageCreatesConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=mydatabase", "driver=postgresql",
				"password=REDACTED", "user=myuser");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
	}

	private void checkDatabaseAccess(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		Object result = DatabaseClient.create(ConnectionFactories.get(connectionFactoryOptions))
			.sql(DatabaseDriver.POSTGRESQL.getValidationQuery())
			.map((row, metadata) -> row.get(0))
			.first()
			.block(Duration.ofSeconds(30));
		assertThat(result).isEqualTo(1);
	}

}
