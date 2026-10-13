/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports when a management port
 * is set.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, args = "--management.server.port=1234")
class ManagementRandomPortWithConfiguredPortSampleActuatorApplicationTests {

	@LocalServerPort
	private int serverPort;

	@LocalManagementPort
	private int managementPort;

	@Test
	void randomPortsShouldBeDifferent() {
		assertThat(this.serverPort).isPositive();
		assertThat(this.managementPort).isPositive();
		assertThat(this.serverPort).isNotEqualTo(this.managementPort);
	}

	@Test
	void randomManagementPortShouldBeApplied() {
		assertThat(this.managementPort).isNotEqualTo(1234);
	}

	@Test
	void testHealth() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/actuator/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
	}

}
