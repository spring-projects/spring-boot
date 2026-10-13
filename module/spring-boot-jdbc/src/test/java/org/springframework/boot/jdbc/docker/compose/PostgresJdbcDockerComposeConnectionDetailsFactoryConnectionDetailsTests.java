/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link PostgresJdbcDockerComposeConnectionDetailsFactory.PostgresJdbcDockerComposeConnectionDetails}.
 *
 * @author Dmytro Nosan
 */
class PostgresJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

	private final RunningService service = mock(RunningService.class);

	private final MockEnvironment environment = new MockEnvironment();

	private final Map<String, String> labels = new LinkedHashMap<>();

	PostgresJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
		given(this.service.env())
			.willReturn(Map.of("POSTGRES_USER", "user", "POSTGRES_PASSWORD", "password", "POSTGRES_DB", "database"));
		given(this.service.labels()).willReturn(this.labels);
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(this.service.ports()).willReturn(connectionPorts);
		given(this.service.host()).willReturn("localhost");
		given(connectionPorts.get(5432)).willReturn(30001);
	}

	@Test
	void createConnectionDetails() {
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("/database");
	}

	@Test
	void createConnectionDetailsWithLabels() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "connectTimeout=30&ApplicationName=spring-boot");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?connectTimeout=30&ApplicationName=spring-boot");
	}

	@Test
	void createConnectionDetailsWithApplicationNameLabelTakesPrecedence() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "ApplicationName=spring-boot");
		this.environment.setProperty("spring.application.name", "my-app");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?ApplicationName=spring-boot");
	}

	@Test
	void createConnectionDetailsWithSpringApplicationName() {
		this.environment.setProperty("spring.application.name", "spring boot");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?ApplicationName=spring+boot");
	}

	@Test
	void createConnectionDetailsAppendSpringApplicationName() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "connectTimeout=30");
		this.environment.setProperty("spring.application.name", "spring boot");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?connectTimeout=30&ApplicationName=spring+boot");
	}

	@Test
	void createConnectionDetailsAppendSpringApplicationNameParametersEndedWithAmpersand() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "connectTimeout=30&");
		this.environment.setProperty("spring.application.name", "spring boot");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?connectTimeout=30&ApplicationName=spring+boot");

	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEqualTo("password");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:postgresql://localhost:30001/database");
		assertThat(connectionDetails.getDriverClassName()).isEqualTo("org.postgresql.Driver");
	}

	private JdbcConnectionDetails getConnectionDetails() {
		return new PostgresJdbcDockerComposeConnectionDetailsFactory.PostgresJdbcDockerComposeConnectionDetails(
				this.service, this.environment);
	}

}
