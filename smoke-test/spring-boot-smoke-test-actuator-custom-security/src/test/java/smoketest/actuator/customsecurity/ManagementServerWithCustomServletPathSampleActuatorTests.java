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

/**
 * Integration tests for a separate management server with a custom dispatcher servlet
 * path.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "spring.mvc.servlet.path=/example" })
class ManagementServerWithCustomServletPathSampleActuatorTests extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void actuatorPathOnMainPortShouldNotMatch() {
		RestTestClient.bindToServer()
			.build()
			.get()
			.uri("http://localhost:" + this.port + "/example/actuator/health")
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Override
	String getPath() {
		return "http://localhost:" + this.port + "/example";
	}

	@Override
	String getActuatorPath() {
		return "http://localhost:" + this.managementPort + "/actuator";
	}

	@Override
	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
