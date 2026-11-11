/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.customsecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

/**
 * Integration tests for actuator endpoints with custom dispatcher servlet path.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.mvc.servlet.path=/example")
class CustomServletPathSampleActuatorTests extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	String getPath() {
		return "http://localhost:" + this.port + "/example";
	}

	@Override
	String getActuatorPath() {
		return "http://localhost:" + this.port + "/example/actuator";
	}

	@Override
	ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
