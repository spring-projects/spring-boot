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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Jersey actuator when not using an isolated {@link ObjectMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "management.endpoints.jackson2.isolated-object-mapper=false")
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
class JerseyActuatorIsolatedObjectMapperFalseTests {

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
			.onStatus(HttpStatusCode::isError, (request, response) -> {
			})
			.toEntity(String.class);
		System.out.println(entity.getBody());
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(entity.getBody())
			.contains("Java 8 date/time type `java.time.Clock$SystemClock` not supported by default");
	}

}
