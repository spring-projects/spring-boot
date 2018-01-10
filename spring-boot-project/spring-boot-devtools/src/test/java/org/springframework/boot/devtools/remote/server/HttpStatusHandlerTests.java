/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.remote.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpStatusHandler}.
 *
 * @author Phillip Webb
 */
public class HttpStatusHandlerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServerHttpResponse response;

	private ServerHttpRequest request;

	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletServerHttpRequest(this.servletRequest);
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}

	@Test
	public void statusMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Status must not be null");
		new HttpStatusHandler(null);
	}

	@Test
	public void respondsOk() throws Exception {
		HttpStatusHandler handler = new HttpStatusHandler();
		handler.handle(this.request, this.response);
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
	}

	@Test
	public void respondsWithStatus() throws Exception {
		HttpStatusHandler handler = new HttpStatusHandler(HttpStatus.I_AM_A_TEAPOT);
		handler.handle(this.request, this.response);
		assertThat(this.servletResponse.getStatus()).isEqualTo(418);
	}

}
