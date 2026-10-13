/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.sql.Driver;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleXeJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
		disabledReason = "The Oracle image has no ARM support")
class OracleXeJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@SuppressWarnings("unchecked")
	@DockerComposeTest(composeFile = "oracle-compose.yaml", image = TestImage.ORACLE_XE)
	void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(JdbcConnectionDetails connectionDetails)
			throws Exception {
		assertThat(connectionDetails.getUsername()).isEqualTo("app_user");
		assertThat(connectionDetails.getPassword()).isEqualTo("app_user_secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:oracle:thin:@").endsWith("/xepdb1");
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
