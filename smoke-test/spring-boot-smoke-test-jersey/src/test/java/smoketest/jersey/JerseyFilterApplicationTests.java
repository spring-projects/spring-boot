/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.springframework.test.context.TestPropertySource;

/**
 * Smoke tests for Jersey configured as a Filter.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = { "spring.jersey.type=filter", "server.servlet.register-default-servlet=true" })
class JerseyFilterApplicationTests extends AbstractJerseyApplicationTests {

}
