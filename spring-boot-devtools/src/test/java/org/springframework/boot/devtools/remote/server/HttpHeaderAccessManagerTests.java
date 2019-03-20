/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.remote.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpHeaderAccessManager}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class HttpHeaderAccessManagerTests {

	private static final String HEADER = "X-AUTH_TOKEN";

	private static final String SECRET = "password";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MockHttpServletRequest request;

	private ServerHttpRequest serverRequest;

	private HttpHeaderAccessManager manager;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest("GET", "/");
		this.serverRequest = new ServletServerHttpRequest(this.request);
		this.manager = new HttpHeaderAccessManager(HEADER, SECRET);
	}

	@Test
	public void headerNameMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderName must not be empty");
		new HttpHeaderAccessManager(null, SECRET);
	}

	@Test
	public void headerNameMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderName must not be empty");
		new HttpHeaderAccessManager("", SECRET);
	}

	@Test
	public void expectedSecretMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ExpectedSecret must not be empty");
		new HttpHeaderAccessManager(HEADER, null);
	}

	@Test
	public void expectedSecretMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ExpectedSecret must not be empty");
		new HttpHeaderAccessManager(HEADER, "");
	}

	@Test
	public void allowsMatching() throws Exception {
		this.request.addHeader(HEADER, SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isTrue();
	}

	@Test
	public void disallowsWrongSecret() throws Exception {
		this.request.addHeader(HEADER, "wrong");
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	public void disallowsNoSecret() throws Exception {
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	public void disallowsWrongHeader() throws Exception {
		this.request.addHeader("X-WRONG", SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

}
