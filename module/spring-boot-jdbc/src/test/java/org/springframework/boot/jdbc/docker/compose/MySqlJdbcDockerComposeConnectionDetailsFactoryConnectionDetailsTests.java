/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.mysql.cj.conf.ConnectionUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link MySqlJdbcDockerComposeConnectionDetailsFactory.MySqlJdbcDockerComposeConnectionDetails}.
 */
class MySqlJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

	private final RunningService service = mock(RunningService.class);

	private final Map<String, String> labels = new LinkedHashMap<>();

	MySqlJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
		given(this.service.env())
			.willReturn(Map.of("MYSQL_USER", "user", "MYSQL_PASSWORD", "password", "MYSQL_DATABASE", "database"));
		given(this.service.labels()).willReturn(this.labels);
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(this.service.ports()).willReturn(connectionPorts);
		given(this.service.host()).willReturn("localhost");
		given(connectionPorts.getTcp(3306)).willReturn(30001);
	}

	@Test
	void createConnectionDetails() {
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("/database");
	}

	@ParameterizedTest
	@ValueSource(strings = { "my+db", "my?db", "my/db", "my%db", "my db", "my数据库" })
	void createConnectionDetailsWithDatabaseNameRequiringEncoding(String database) {
		given(this.service.env())
			.willReturn(Map.of("MYSQL_USER", "user", "MYSQL_PASSWORD", "password", "MYSQL_DATABASE", database));
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertThat(
				ConnectionUrl.getConnectionUrlInstance(connectionDetails.getJdbcUrl(), new Properties()).getDatabase())
			.isEqualTo(database);
	}

	@Test
	void createConnectionDetailsWithLabels() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "connectTimeout=30");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?connectTimeout=30");
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEqualTo("password");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:mysql://localhost:30001/database");
	}

	private JdbcConnectionDetails getConnectionDetails() {
		return new MySqlJdbcDockerComposeConnectionDetailsFactory.MySqlJdbcDockerComposeConnectionDetails(this.service);
	}

}
