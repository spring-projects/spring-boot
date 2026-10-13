/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.jersey;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Integration tests for actuator endpoints with custom application path.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.jersey.application-path=/example")
class CustomApplicationPathActuatorTests extends AbstractJerseySecureTests {

	@LocalServerPort
	private int port;

	@Override
	String getPath() {
		return "http://localhost:" + this.port + "/example";
	}

	@Override
	String getManagementPath() {
		return "http://localhost:" + this.port + "/example";
	}

}
