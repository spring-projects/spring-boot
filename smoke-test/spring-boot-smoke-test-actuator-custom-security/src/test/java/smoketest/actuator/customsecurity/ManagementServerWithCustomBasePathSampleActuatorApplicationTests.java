/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.customsecurity;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for a separate management server with a custom base path.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.server.base-path=/management" })
class ManagementServerWithCustomBasePathSampleActuatorApplicationTests
		extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testMissing() {
		RestTestClient.bindToServer()
			.defaultHeaders((headers) -> headers.setBasicAuth("admin", "admin"))
			.build()
			.get()
			.uri(getActuatorPath() + "/missing")
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":404"));
	}

	@Override
	String getPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	String getActuatorPath() {
		return "http://localhost:" + this.managementPort + "/management/actuator";
	}

	@Override
	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
