/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.sql.Driver;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
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
 * @author He Zean
 */
class PostgresJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "postgres-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		assertConnectionDetails(connectionDetails);
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-with-trust-host-auth-method-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetailsThatCanAccessDatabaseWhenHostAuthMethodIsTrust(
			JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isNull();
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
		checkDatabaseAccess(connectionDetails);
	}

	@DockerComposeTest(composeFile = "postgres-application-name-compose.yaml", image = TestImage.POSTGRESQL)
	void runCreatesConnectionDetailsApplicationName(JdbcConnectionDetails connectionDetails)
			throws ClassNotFoundException {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://")
			.endsWith("?ApplicationName=spring+boot");
		assertThat(executeQuery(connectionDetails, "select current_setting('application_name')", String.class))
			.isEqualTo("spring boot");
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://").endsWith("/mydatabase");
	}

	private void checkDatabaseAccess(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
		String validationQuery = DatabaseDriver.POSTGRESQL.getValidationQuery();
		assertThat(validationQuery).isNotNull();
		assertThat(executeQuery(connectionDetails, validationQuery, Integer.class)).isEqualTo(1);
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T executeQuery(JdbcConnectionDetails connectionDetails, String sql, Class<T> resultClass)
			throws ClassNotFoundException {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setUrl(connectionDetails.getJdbcUrl());
		dataSource.setUsername(connectionDetails.getUsername());
		dataSource.setPassword(connectionDetails.getPassword());
		dataSource.setDriverClass((Class<? extends Driver>) ClassUtils.forName(connectionDetails.getDriverClassName(),
				getClass().getClassLoader()));
		return new JdbcTemplate(dataSource).queryForObject(sql, resultClass);
	}

}
