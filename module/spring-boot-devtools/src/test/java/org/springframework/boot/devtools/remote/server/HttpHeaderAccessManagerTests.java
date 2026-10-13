/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
class HttpHeaderAccessManagerTests {

	private static final String HEADER = "X-AUTH_TOKEN";

	private static final String SECRET = "password";

	private MockHttpServletRequest request;

	private ServerHttpRequest serverRequest;

	private HttpHeaderAccessManager manager;

	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest("GET", "/");
		this.serverRequest = new ServletServerHttpRequest(this.request);
		this.manager = new HttpHeaderAccessManager(HEADER, SECRET);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void headerNameMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpHeaderAccessManager(null, SECRET))
			.withMessageContaining("'headerName' must not be empty");
	}

	@Test
	void headerNameMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpHeaderAccessManager("", SECRET))
			.withMessageContaining("'headerName' must not be empty");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void expectedSecretMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpHeaderAccessManager(HEADER, null))
			.withMessageContaining("'expectedSecret' must not be empty");
	}

	@Test
	void expectedSecretMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpHeaderAccessManager(HEADER, ""))
			.withMessageContaining("'expectedSecret' must not be empty");
	}

	@Test
	void allowsMatching() {
		this.request.addHeader(HEADER, SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isTrue();
	}

	@Test
	void disallowsWrongSecret() {
		this.request.addHeader(HEADER, "wrong");
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	void disallowsNoSecret() {
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

	@Test
	void disallowsWrongHeader() {
		this.request.addHeader("X-WRONG", SECRET);
		assertThat(this.manager.isAllowed(this.serverRequest)).isFalse();
	}

}
