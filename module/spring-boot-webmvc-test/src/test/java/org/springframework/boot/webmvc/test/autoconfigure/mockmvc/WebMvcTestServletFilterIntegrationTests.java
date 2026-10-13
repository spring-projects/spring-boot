/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} servlet filter registration.
 *
 * @author Phillip Webb
 */
@WebMvcTest
class WebMvcTestServletFilterIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldApplyFilter() {
		assertThat(this.mvc.get().uri("/one")).hasHeader("x-test", "abc");
	}

}
