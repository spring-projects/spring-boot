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
