/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for Jersey configured as a Servlet using separate management and main
 * service ports.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = { "spring.jersey.type=filter", "server.servlet.register-default-servlet=true" })
class JerseyFilterManagementPortTests extends AbstractJerseyManagementPortTests {

}
