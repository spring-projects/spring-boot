/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * {@link ClickHouseJdbcDockerComposeConnectionDetailsFactory.ClickhouseJdbcDockerComposeConnectionDetails}.
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

	private final RunningService service = mock(RunningService.class);

	private final Map<String, String> labels = new LinkedHashMap<>();

	ClickHouseJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
		given(this.service.env()).willReturn(
				Map.of("CLICKHOUSE_USER", "user", "CLICKHOUSE_PASSWORD", "password", "CLICKHOUSE_DB", "database"));
		given(this.service.labels()).willReturn(this.labels);
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(this.service.ports()).willReturn(connectionPorts);
		given(this.service.host()).willReturn("localhost");
		given(connectionPorts.getTcp(8123)).willReturn(30001);
	}

	@Test
	void createConnectionDetails() {
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("/database");
	}

	@ParameterizedTest
	@ValueSource(strings = { "my?db", "my/db", "my%db", "my db" })
	void createConnectionDetailsWithDatabaseNameRequiringEncoding(String database) {
		given(this.service.env()).willReturn(
				Map.of("CLICKHOUSE_USER", "user", "CLICKHOUSE_PASSWORD", "password", "CLICKHOUSE_DB", database));
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		// The driver percent-decodes the database segment; a '+' stays literal, so spaces
		// must be encoded as %20 rather than the x-www-form-urlencoded '+'
		assertThat(connectionDetails.getJdbcUrl()).endsWith("/" + percentEncode(database));
	}

	@Test
	void createConnectionDetailsWithLabels() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "connect_timeout=30");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith("?connect_timeout=30");
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEqualTo("password");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:clickhouse://localhost:30001/database");
	}

	private JdbcConnectionDetails getConnectionDetails() {
		return new ClickHouseJdbcDockerComposeConnectionDetailsFactory.ClickhouseJdbcDockerComposeConnectionDetails(
				this.service);
	}

	static String percentEncode(String value) {
		return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
	}

}
