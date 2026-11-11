/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests application for MockMvc Security.
 *
 * @author Andy Wilkinson
 */
@SpringBootApplication
public class SecurityTestApplication {

	@RestController
	static class MyController {

		@RequestMapping("/")
		@Secured("ROLE_USER")
		String index() {
			return "Hello";
		}

	}

}
