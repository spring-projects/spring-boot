/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.docker.compose;

import java.sql.Driver;
import java.time.Duration;

import org.awaitility.Awaitility;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleFreeJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class OracleFreeJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@SuppressWarnings("unchecked")
	@DockerComposeTest(composeFile = "oracle-compose.yaml", image = TestImage.ORACLE_FREE)
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(JdbcConnectionDetails connectionDetails)
			throws Exception {
		assertThat(connectionDetails.getUsername()).isEqualTo("app_user");
		assertThat(connectionDetails.getPassword()).isEqualTo("app_user_secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:oracle:thin:@").endsWith("/freepdb1");
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setUrl(connectionDetails.getJdbcUrl());
		dataSource.setUsername(connectionDetails.getUsername());
		dataSource.setPassword(connectionDetails.getPassword());
		dataSource.setDriverClass((Class<? extends Driver>) ClassUtils.forName(connectionDetails.getDriverClassName(),
				getClass().getClassLoader()));
		Awaitility.await().atMost(Duration.ofMinutes(1)).ignoreExceptions().untilAsserted(() -> {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			String validationQuery = DatabaseDriver.ORACLE.getValidationQuery();
			assertThat(validationQuery).isNotNull();
			assertThat(template.queryForObject(validationQuery, String.class)).isEqualTo("Hello");
		});
	}

}
