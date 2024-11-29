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

import java.sql.Driver;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PostgresJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "postgres-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-with-trust-host-auth-method-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetailsThatCanAccessDatabaseWhenHostAuthMethodIsTrust(
			JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isNull();
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-bitnami-compose.yaml", image = TestImage.BITNAMI_POSTGRESQL)
	void runWithBitnamiImageCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
	}

	@SuppressWarnings("unchecked")
	private void checkDatabaseAccess(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setUrl(connectionDetails.getJdbcUrl());
		dataSource.setUsername(connectionDetails.getUsername());
		dataSource.setPassword(connectionDetails.getPassword());
		dataSource.setDriverClass((Class<? extends Driver>) ClassUtils.forName(connectionDetails.getDriverClassName(),
				getClass().getClassLoader()));
		JdbcTemplate template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject(DatabaseDriver.POSTGRESQL.getValidationQuery(), Integer.class)).isEqualTo(1);
	}

}
