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

package org.springframework.boot.test.autoconfigure.security;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MockMvc security.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
@TestPropertySource(properties = { "debug=true" })
class MockMvcSecurityIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@WithMockUser(username = "test", password = "test", roles = "USER")
	void okResponseWithMockUser() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk();
	}

	@Test
	void unauthorizedResponseWithNoUser() {
		assertThat(this.mvc.get().uri("/").accept(MediaType.APPLICATION_JSON)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void okResponseWithBasicAuthCredentialsForKnownUser() {
		assertThat(this.mvc.get()
			.uri("/")
			.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes())))
			.hasStatusOk();
	}

}
