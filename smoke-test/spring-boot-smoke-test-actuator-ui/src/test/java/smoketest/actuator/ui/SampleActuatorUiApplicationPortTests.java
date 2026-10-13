/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.ui;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port:0" })
class SampleActuatorUiApplicationPortTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void testHome() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.port)
			.build()
			.get()
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void testMetrics() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.build()
			.get()
			.uri("/actuator/metrics")
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void testHealth() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", getPassword()))
			.build()
			.get()
			.uri("/actuator/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
	}

	private String getPassword() {
		return "password";
	}

}
