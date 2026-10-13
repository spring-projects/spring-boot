/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
		ResponseEntity<String> entity = new TestRestTemplate().withBasicAuth("user", "password")
			.getForEntity("http://localhost:" + this.managementPort + "/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

}
