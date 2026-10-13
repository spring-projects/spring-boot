/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.customsecurity;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for a separate management server with custom base path and web
 * endpoints base path.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=0",
		"management.server.base-path=/management", "management.endpoints.web.base-path=/" })
class ManagementServerWithCustomBasePathAndWebEndpointsBasePathSampleActuatorApplicationTests
		extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testMissing() {
		ResponseEntity<String> entity = new TestRestTemplate("admin", "admin")
			.getForEntity(getActuatorPath() + "/missing", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(entity.getBody()).contains("\"status\":404");
	}

	@Override
	String getPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	String getActuatorPath() {
		return "http://localhost:" + this.managementPort + "/management";
	}

	@Override
	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
