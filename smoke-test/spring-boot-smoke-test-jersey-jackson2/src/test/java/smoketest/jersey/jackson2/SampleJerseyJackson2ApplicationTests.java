/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJerseyJackson2Application} with an isolated endpoint
 * {@code ObjectMapper}.
 *
 * @author Kristoffer Larsen Hopland
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.jackson2.isolated-object-mapper=true" })
class SampleJerseyJackson2ApplicationTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void resourceIsSerializedUsingAutoConfiguredObjectMapper() {
		assertThat(get(this.port, "/name")).isEqualTo("{\"first_name\":\"Spring\",\"last_name\":\"Boot\"}");
	}

	@Test
	void endpointIsSerializedUsingIsolatedObjectMapper() {
		assertThat(get(this.port, "/actuator/names")).isEqualTo("{\"firstName\":\"Spring\",\"lastName\":\"Boot\"}");
	}

	private String get(int port, String path) {
		String body = RestClient.create().get().uri("http://localhost:" + port + path).retrieve().body(String.class);
		assertThat(body).isNotNull();
		return body;
	}

}
