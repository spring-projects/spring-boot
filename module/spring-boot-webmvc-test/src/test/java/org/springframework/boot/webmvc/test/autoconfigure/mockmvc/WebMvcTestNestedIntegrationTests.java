/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} using {@link Nested}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest(controllers = ExampleController2.class)
class WebMvcTestNestedIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldNotFindController1() {
		assertThat(this.mvc.get().uri("/one")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldFindController2() {
		assertThat(this.mvc.get().uri("/two")).hasStatusOk().hasBodyTextEqualTo("hellotwo");
	}

	@Nested
	class NestedTests {

		@Test
		void shouldNotFindController1() {
			assertThat(WebMvcTestNestedIntegrationTests.this.mvc.get().uri("/one")).hasStatus(HttpStatus.NOT_FOUND);
		}

		@Test
		void shouldFindController2() {
			assertThat(WebMvcTestNestedIntegrationTests.this.mvc.get().uri("/two")).hasStatusOk()
				.hasBodyTextEqualTo("hellotwo");
		}

	}

}
