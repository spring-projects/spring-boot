/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Test {@link DispatcherServlet} customizations.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest
@TestPropertySource(properties = { "spring.mvc.throw-exception-if-no-handler-found=true",
		"spring.mvc.static-path-pattern=/static/**" })
class WebMvcTestCustomDispatcherServletIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void dispatcherServletIsCustomized() {
		assertThat(this.mvc.get().uri("/does-not-exist")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasBodyTextEqualTo("Invalid request: /does-not-exist");
	}

}
