/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link PostgresR2dbcDockerComposeConnectionDetailsFactory.PostgresDbR2dbcDockerComposeConnectionDetails}.
 *
 * @author Dmytro Nosan
 */
class PostgresR2dbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

	private static final Option<String> APPLICATION_NAME = Option.valueOf("applicationName");

	private final RunningService service = mock(RunningService.class);

	private final MockEnvironment environment = new MockEnvironment();

	private final Map<String, String> labels = new LinkedHashMap<>();

	PostgresR2dbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
		given(this.service.env())
			.willReturn(Map.of("POSTGRES_USER", "myuser", "POSTGRES_PASSWORD", "secret", "POSTGRES_DB", "mydatabase"));
		given(this.service.labels()).willReturn(this.labels);
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(this.service.ports()).willReturn(connectionPorts);
		given(this.service.host()).willReturn("localhost");
		given(connectionPorts.get(5432)).willReturn(30001);
	}

	@Test
	void createConnectionDetails() {
		ConnectionFactoryOptions options = getConnectionFactoryOptions();
		assertConnectionFactoryOptions(options);
		assertThat(options.getValue(APPLICATION_NAME)).isNull();
	}

	@Test
	void createConnectionDetailsWithLabels() {
		this.labels.put("org.springframework.boot.r2dbc.parameters",
				"connectTimeout=PT15S,applicationName=spring-boot");
		ConnectionFactoryOptions options = getConnectionFactoryOptions();
		assertConnectionFactoryOptions(options);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.CONNECT_TIMEOUT)).isEqualTo("PT15S");
		assertThat(options.getRequiredValue(APPLICATION_NAME)).isEqualTo("spring-boot");
	}

	@Test
	void createConnectionDetailsWithApplicationNameLabelTakesPrecedence() {
		this.labels.put("org.springframework.boot.r2dbc.parameters", "applicationName=spring-boot");
		this.environment.setProperty("spring.application.name", "my-app");
		ConnectionFactoryOptions options = getConnectionFactoryOptions();
		assertConnectionFactoryOptions(options);
		assertThat(options.getRequiredValue(APPLICATION_NAME)).isEqualTo("spring-boot");
	}

	@Test
	void createConnectionDetailsWithSpringApplicationName() {
		this.environment.setProperty("spring.application.name", "spring boot");
		ConnectionFactoryOptions options = getConnectionFactoryOptions();
		assertConnectionFactoryOptions(options);
		assertThat(options.getRequiredValue(APPLICATION_NAME)).isEqualTo("spring boot");
	}

	@Test
	void createConnectionDetailsAppendSpringApplicationName() {
		this.labels.put("org.springframework.boot.r2dbc.parameters", "connectTimeout=PT15S");
		this.environment.setProperty("spring.application.name", "my-app");
		ConnectionFactoryOptions options = getConnectionFactoryOptions();
		assertConnectionFactoryOptions(options);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.CONNECT_TIMEOUT)).isEqualTo("PT15S");
		assertThat(options.getRequiredValue(APPLICATION_NAME)).isEqualTo("my-app");
	}

	private void assertConnectionFactoryOptions(ConnectionFactoryOptions options) {
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("localhost");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PORT)).isEqualTo(30001);
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydatabase");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("myuser");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("secret");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("postgresql");
	}

	private ConnectionFactoryOptions getConnectionFactoryOptions() {
		return new PostgresR2dbcDockerComposeConnectionDetailsFactory.PostgresDbR2dbcDockerComposeConnectionDetails(
				this.service, this.environment)
			.getConnectionFactoryOptions();
	}

}
