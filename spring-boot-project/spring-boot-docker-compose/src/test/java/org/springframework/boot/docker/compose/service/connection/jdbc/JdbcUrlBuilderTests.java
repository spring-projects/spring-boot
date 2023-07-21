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

package org.springframework.boot.docker.compose.service.connection.jdbc;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcUrlBuilder}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class JdbcUrlBuilderTests {

	private JdbcUrlBuilder builder = new JdbcUrlBuilder("mydb", 1234);

	@Test
	void createWhenDriverProtocolIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JdbcUrlBuilder(null, 123))
			.withMessage("DriverProtocol must not be null");
	}

	@Test
	void buildBuildsUrlForService() {
		RunningService service = mockService(456);
		String url = this.builder.build(service);
		assertThat(url).isEqualTo("jdbc:mydb://myhost:456");
	}

	@Test
	void buildBuildsUrlForServiceAndDatabase() {
		RunningService service = mockService(456);
		String url = this.builder.build(service, "mydb");
		assertThat(url).isEqualTo("jdbc:mydb://myhost:456/mydb");
	}

	@Test
	void buildWhenHasParamsLabelBuildsUrl() {
		RunningService service = mockService(456, Map.of("org.springframework.boot.jdbc.parameters", "foo=bar"));
		String url = this.builder.build(service, "mydb");
		assertThat(url).isEqualTo("jdbc:mydb://myhost:456/mydb?foo=bar");
	}

	@Test
	void buildWhenServiceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.build(null, "mydb"))
			.withMessage("Service must not be null");
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
