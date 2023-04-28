/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.oracle;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleR2dbcDockerComposeConnectionDetailsFactory}
 *
 * @author Andy Wilkinson
 */
class OracleR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	OracleR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("oracle-compose.yaml");
	}

	@Test
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase() {
		R2dbcConnectionDetails connectionDetails = run(R2dbcConnectionDetails.class);
		ConnectionFactoryOptions connectionFactoryOptions = connectionDetails.getConnectionFactoryOptions();
		assertThat(connectionFactoryOptions.toString()).contains("database=xepdb1", "driver=oracle",
				"password=REDACTED", "user=system");
		assertThat(connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		Object result = DatabaseClient.create(ConnectionFactories.get(connectionFactoryOptions))
			.sql(DatabaseDriver.ORACLE.getValidationQuery())
			.map((row, metadata) -> row.get(0))
			.first()
			.block(Duration.ofSeconds(30));
		assertThat(result).isEqualTo("Hello");
	}

}
