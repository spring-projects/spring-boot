/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Jersey actuator when using an isolated {@link ObjectMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "management.endpoints.jackson.isolated-json-mapper=true")
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
class JerseyActuatorIsolatedObjectMapperTrueTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void resourceShouldBeAvailableOnMainPort() {
		ResponseEntity<String> entity = RestClient.create()
			.get()
			.uri("http://localhost:" + this.port + "/actuator/startup")
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"timeline\":");
	}

}
