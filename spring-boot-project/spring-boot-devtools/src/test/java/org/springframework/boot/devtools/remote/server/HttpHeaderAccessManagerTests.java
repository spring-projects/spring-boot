/*
 * Copyright 2012-2018 the original author or authors.
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
import org.junit.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link HttpHeaderAccessManager}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class HttpHeaderAccessManagerTests {

	private static final String HEADER = "X-AUTH_TOKEN";

	private static final String SECRET = "password";

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
	public void headerNameMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpHeaderAccessManager(null, SECRET))
				.withMessageContaining("HeaderName must not be empty");
	}

	@Test
	public void headerNameMustNotBeEmpty() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpHeaderAccessManager("", SECRET))
				.withMessageContaining("HeaderName must not be empty");
	}

	@Test
	public void expectedSecretMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpHeaderAccessManager(HEADER, null))
				.withMessageContaining("ExpectedSecret must not be empty");
	}

	@Test
	public void expectedSecretMustNotBeEmpty() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpHeaderAccessManager(HEADER, ""))
				.withMessageContaining("ExpectedSecret must not be empty");
	}

	@Test
	public void allowsMatching() {
		this.request.addHeader(HEADER, SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isTrue();
	}

	@Test
	public void disallowsWrongSecret() {
		this.request.addHeader(HEADER, "wrong");
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	public void disallowsNoSecret() {
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	public void disallowsWrongHeader() {
		this.request.addHeader("X-WRONG", SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

}
