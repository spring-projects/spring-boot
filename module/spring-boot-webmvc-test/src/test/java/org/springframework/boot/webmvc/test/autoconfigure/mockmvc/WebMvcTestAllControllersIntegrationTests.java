/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when no explicit controller is defined.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@WebMvcTest
class WebMvcTestAllControllersIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired(required = false)
	private ErrorAttributes errorAttributes;

	@Test
	void shouldFindController1() {
		assertThat(this.mvc.get().uri("/one")).satisfies(hasBody("one"));
	}

	@Test
	void shouldFindController2() {
		assertThat(this.mvc.get().uri("/two")).satisfies(hasBody("hellotwo"));
	}

	@Test
	void shouldFindControllerAdvice() {
		assertThat(this.mvc.get().uri("/error")).satisfies(hasBody("recovered"));
	}

	@Test
	void shouldRunValidationSuccess() {
		assertThat(this.mvc.get().uri("/three/OK")).satisfies(hasBody("Hello OK"));
	}

	@Test
	void shouldRunValidationFailure() {
		assertThat(this.mvc.get().uri("/three/invalid")).failure()
			.isInstanceOf(ServletException.class)
			.hasCauseInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void shouldNotFilterErrorAttributes() {
		assertThat(this.errorAttributes).isNotNull();

	}

	private Consumer<MvcTestResult> hasBody(String expected) {
		return (result) -> assertThat(result).hasStatusOk().hasBodyTextEqualTo(expected);
	}

}
