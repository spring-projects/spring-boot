/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Tests to ensure that the error page with a custom context path is accessible only to
 * authorized users.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class, ErrorPageTests.SecurityConfiguration.class,
				SampleWebSecureApplication.class },
		properties = { "spring.web.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password", "server.servlet.context-path=/example" })
class CustomContextPathErrorPageTests extends AbstractErrorPageTests {

	CustomContextPathErrorPageTests() {
		super("");
	}

}
