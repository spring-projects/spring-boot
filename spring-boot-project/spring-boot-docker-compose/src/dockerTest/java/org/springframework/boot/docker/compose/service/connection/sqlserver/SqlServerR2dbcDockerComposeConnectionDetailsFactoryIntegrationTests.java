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

package org.springframework.boot.docker.compose.service.connection.sqlserver;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SqlServerR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
		disabledReason = "The SQL server image has no ARM support")
class SqlServerR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "mssqlserver-compose.yaml", image = TestImage.SQL_SERVER)
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("driver=mssql", "password=REDACTED", "user=SA");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD))
			.isEqualTo("verYs3cret");
		Object result = DatabaseClient.create(ConnectionFactories.get(connectionFactoryOptions))
			.sql(DatabaseDriver.SQLSERVER.getValidationQuery())
			.map((row, metadata) -> row.get(0))
			.first()
			.block(Duration.ofSeconds(30));
		assertThat(result).isEqualTo(1);
	}

}
