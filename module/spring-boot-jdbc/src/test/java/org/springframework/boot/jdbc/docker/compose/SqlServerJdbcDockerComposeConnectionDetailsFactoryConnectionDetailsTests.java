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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link SqlServerJdbcDockerComposeConnectionDetailsFactory.SqlServerJdbcDockerComposeConnectionDetails}.
 *
 * @author 2heunxun
 */
class SqlServerJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

	private final RunningService service = mock(RunningService.class);

	private final Map<String, String> labels = new LinkedHashMap<>();

	SqlServerJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
		given(this.service.env()).willReturn(Map.of("MSSQL_SA_PASSWORD", "verYs3cret"));
		given(this.service.labels()).willReturn(this.labels);
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(this.service.ports()).willReturn(connectionPorts);
		given(this.service.host()).willReturn("localhost");
		given(connectionPorts.get(1433)).willReturn(30001);
	}

	@Test
	void createConnectionDetails() {
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).endsWith(";encrypt=false");
	}

	@Test
	void createConnectionDetailsWithLabels() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "sendStringParametersAsUnicode=false");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).contains(";sendStringParametersAsUnicode=false;")
			.endsWith(";encrypt=false");
	}

	@Test
	void createConnectionDetailsWithEncryptLabelDoesNotOverrideUserChoice() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "encrypt=true");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).containsOnlyOnce("encrypt=").endsWith(";encrypt=true");
	}

	@Test
	void createConnectionDetailsWithEncryptFalseLabelIsNotDuplicated() {
		this.labels.put("org.springframework.boot.jdbc.parameters", "encrypt=false");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).containsOnlyOnce("encrypt=").endsWith(";encrypt=false");
	}

	@Test
	void createConnectionDetailsWithEncryptLabelAmongOtherParametersDoesNotOverrideUserChoice() {
		this.labels.put("org.springframework.boot.jdbc.parameters",
				"sendStringParametersAsUnicode=false;encrypt=true;loginTimeout=30");
		JdbcConnectionDetails connectionDetails = getConnectionDetails();
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getJdbcUrl()).containsOnlyOnce("encrypt=")
			.endsWith(";sendStringParametersAsUnicode=false;encrypt=true;loginTimeout=30");
	}

	private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("SA");
		assertThat(connectionDetails.getPassword()).isEqualTo("verYs3cret");
		assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:sqlserver://localhost:30001");
		assertThat(connectionDetails.getDriverClassName()).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	}

	private JdbcConnectionDetails getConnectionDetails() {
		return new SqlServerJdbcDockerComposeConnectionDetailsFactory.SqlServerJdbcDockerComposeConnectionDetails(
				this.service);
	}

}
