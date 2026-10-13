/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=0",
		"management.endpoints.web.base-path=/admin", "management.endpoint.health.show-details=never" })
class ManagementPortAndPathWithAntPatcherSampleActuatorApplicationTests
		extends AbstractManagementPortAndPathSampleActuatorApplicationTests {

}
