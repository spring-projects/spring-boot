/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import java.util.Collections;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;

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

	private final ConnectionFactoryOptionsBuilder builder = new ConnectionFactoryOptionsBuilder("mydb", 1234);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenDriverIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConnectionFactoryOptionsBuilder(null, 123))
			.withMessage("'driver' must not be null");
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
	@SuppressWarnings("NullAway") // Test null check
	void buildWhenServiceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.build(null, "mydb", "user", "pass"))
			.withMessage("'service' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void buildWhenDatabaseIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.build(mockService(456), null, "user", "pass"))
			.withMessage("'database' must not be null");
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
