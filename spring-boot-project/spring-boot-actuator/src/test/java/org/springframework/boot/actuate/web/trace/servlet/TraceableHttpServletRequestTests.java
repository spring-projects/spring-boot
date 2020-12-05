/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.web.trace.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TraceableHttpServletRequest}.
 *
 * @author Madhura Bhave
 */
class TraceableHttpServletRequestTests {

	private MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest("GET", "/script");
	}

	@Test
	void getUriWithoutQueryStringShouldReturnUri() {
		validate("http://localhost/script");
	}

	@Test
	void getUriShouldReturnUriWithQueryString() {
		this.request.setQueryString("a=b");
		validate("http://localhost/script?a=b");
	}

	@Test
	void getUriWithSpecialCharactersInQueryStringShouldEncode() {
		this.request.setQueryString("a=${b}");
		validate("http://localhost/script?a=$%7Bb%7D");
	}

	@Test
	void getUriWithSpecialCharactersEncodedShouldNotDoubleEncode() {
		this.request.setQueryString("a=$%7Bb%7D");
		validate("http://localhost/script?a=$%7Bb%7D");
	}

	private void validate(String expectedUri) {
		TraceableHttpServletRequest trace = new TraceableHttpServletRequest(this.request);
		assertThat(trace.getUri().toString()).isEqualTo(expectedUri);
	}

}
