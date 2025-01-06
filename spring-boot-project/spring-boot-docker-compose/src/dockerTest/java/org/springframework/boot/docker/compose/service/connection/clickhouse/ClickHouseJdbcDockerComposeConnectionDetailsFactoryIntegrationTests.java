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
 * Integration tests for {@link ClickHouseJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "clickhouse-compose.yaml", image = TestImage.CLICKHOUSE)
	void runCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		assertConnectionDetails(connectionDetails);
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "clickhouse-bitnami-compose.yaml", image = TestImage.BITNAMI_CLICKHOUSE)
	void runWithBitnamiImageCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		// See https://github.com/bitnami/containers/issues/73550
		// checkDatabaseAccess(connectionDetails);
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:clickhouse://").endsWith("/mydatabase");
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
		assertThat(template.queryForObject(DatabaseDriver.CLICKHOUSE.getValidationQuery(), Integer.class)).isEqualTo(1);
	}

}
