/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.security.test.context.support.WithMockUser;
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
@WithMockUser
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
