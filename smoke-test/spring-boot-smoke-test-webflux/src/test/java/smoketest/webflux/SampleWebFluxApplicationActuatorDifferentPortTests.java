/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webflux;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports with empty endpoint
 * base path.
 *
 * @author HaiTao Zhang
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoints.web.base-path=/" })
class SampleWebFluxApplicationActuatorDifferentPortTests {

	@LocalManagementPort
	private int managementPort;

	@Test
	void linksEndpointShouldBeAvailable() {
		WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.build()
			.get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value((response) -> assertThat(response).contains("\"_links\""));
	}

	private String getPassword() {
		return "password";
	}

}
