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

package org.springframework.boot.docker.compose.service.connection.clickhouse;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClickHouseR2dbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Dmytro Nosan
 */
class ClickHouseR2dbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "clickhouse-defaults-compose.yaml", image = TestImage.CLICKHOUSE)
	void runCreatesR2dbcConnectionDetailsWithDefaultValues(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions options = connectionDetails.getConnectionFactoryOptions();
		assertCommonOptions(options);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("default");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("default");
		assertThat(options.hasOption(ConnectionFactoryOptions.PASSWORD)).isFalse();
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "clickhouse-compose.yaml", image = TestImage.CLICKHOUSE)
	void runCreatesR2dbcConnectionDetails(R2dbcConnectionDetails connectionDetails) {
		ConnectionFactoryOptions options = connectionDetails.getConnectionFactoryOptions();
		assertCommonOptions(options);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("myuser");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		checkDatabaseAccess(connectionDetails);
	}

	private void assertCommonOptions(ConnectionFactoryOptions options) {
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isNotNull();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isNotNull();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("clickhouse");
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
