/*
 * Copyright 2012-2022 the original author or authors.
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

import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	private MockMvc mvc;

	@Autowired(required = false)
	private ErrorAttributes errorAttributes;

	@Test
	void shouldFindController1() throws Exception {
		this.mvc.perform(get("/one")).andExpect(content().string("one")).andExpect(status().isOk());
	}

	@Test
	void shouldFindController2() throws Exception {
		this.mvc.perform(get("/two")).andExpect(content().string("hellotwo")).andExpect(status().isOk());
	}

	@Test
	void shouldFindControllerAdvice() throws Exception {
		this.mvc.perform(get("/error")).andExpect(content().string("recovered")).andExpect(status().isOk());
	}

	@Test
	void shouldRunValidationSuccess() throws Exception {
		this.mvc.perform(get("/three/OK")).andExpect(status().isOk()).andExpect(content().string("Hello OK"));
	}

	@Test
	void shouldRunValidationFailure() {
		assertThatExceptionOfType(ServletException.class).isThrownBy(() -> this.mvc.perform(get("/three/invalid")))
				.withCauseInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void shouldNotFilterErrorAttributes() {
		assertThat(this.errorAttributes).isNotNull();

	}

}
