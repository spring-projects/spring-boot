/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.r2dbc;

import java.util.Collections;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionFactoryOptionsBuilder}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConnectionFactoryOptionsBuilderTests {

	private ConnectionFactoryOptionsBuilder builder = new ConnectionFactoryOptionsBuilder("mydb", 1234);

	@Test
	void createWhenDriverProtocolIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JdbcUrlBuilder(null, 123))
			.withMessage("DriverProtocol must not be null");
	}

	@Test
	void buildBuildsOptions() {
		RunningService service = mockService(456);
		ConnectionFactoryOptions options = this.builder.build(service, "mydb", "user", "pass");
		assertThat(options).isEqualTo(ConnectionFactoryOptions.builder()
			.option(ConnectionFactoryOptions.DATABASE, "mydb")
			.option(ConnectionFactoryOptions.HOST, "myhost")
			.option(ConnectionFactoryOptions.PORT, 456)
			.option(ConnectionFactoryOptions.DRIVER, "mydb")
			.option(ConnectionFactoryOptions.PASSWORD, "pass")
			.option(ConnectionFactoryOptions.USER, "user")
			.build());
	}

	@Test
	void buildWhenHasParamsLabelBuildsOptions() {
		RunningService service = mockService(456, Map.of("org.springframework.boot.r2dbc.parameters", "foo=bar"));
		ConnectionFactoryOptions options = this.builder.build(service, "mydb", "user", "pass");
		assertThat(options).isEqualTo(ConnectionFactoryOptions.builder()
			.option(ConnectionFactoryOptions.DATABASE, "mydb")
			.option(ConnectionFactoryOptions.HOST, "myhost")
			.option(ConnectionFactoryOptions.PORT, 456)
			.option(ConnectionFactoryOptions.DRIVER, "mydb")
			.option(ConnectionFactoryOptions.PASSWORD, "pass")
			.option(ConnectionFactoryOptions.USER, "user")
			.option(Option.valueOf("foo"), "bar")
			.build());
	}

	@Test
	void buildWhenServiceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.build(null, "mydb", "user", "pass"))
			.withMessage("Service must not be null");
	}

	@Test
	void buildWhenDatabaseIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.build(mockService(456), null, "user", "pass"))
			.withMessage("Database must not be null");
	}

	private RunningService mockService(int mappedPort) {
		return mockService(mappedPort, Collections.emptyMap());
	}

	private RunningService mockService(int mappedPort, Map<String, String> labels) {
		RunningService service = mock(RunningService.class);
		ConnectionPorts ports = mock(ConnectionPorts.class);
		given(ports.get(1234)).willReturn(mappedPort);
		given(service.host()).willReturn("myhost");
		given(service.ports()).willReturn(ports);
		given(service.labels()).willReturn(labels);
		return service;
	}

}
