/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

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

	private final JdbcUrlBuilder builder = new JdbcUrlBuilder("mydb", 1234);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenDriverProtocolIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JdbcUrlBuilder(null, 123))
			.withMessage("'driverProtocol' must not be null");
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
	void buildWithCustomAppendParametersWhenHasParamsLabelBuildsUrl() {
		RunningService service = mockService(456, Map.of("org.springframework.boot.jdbc.parameters", "foo=bar"));
		String url = new JdbcUrlBuilder("mydb", 1234) {

			@Override
			protected void appendParameters(StringBuilder url, String parameters) {
				url.append(";").append(parameters);
			}

		}.build(service, "mydb");
		assertThat(url).isEqualTo("jdbc:mydb://myhost:456/mydb;foo=bar");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void buildWhenServiceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.build(null, "mydb"))
			.withMessage("'service' must not be null");
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
