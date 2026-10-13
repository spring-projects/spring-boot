/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link HttpStatusHandler}.
 *
 * @author Phillip Webb
 */
class HttpStatusHandlerTests {

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServerHttpResponse response;

	private ServerHttpRequest request;

	@BeforeEach
	void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletServerHttpRequest(this.servletRequest);
		this.response = new ServletServerHttpResponse(this.servletResponse);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void statusMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpStatusHandler(null))
			.withMessageContaining("'status' must not be null");
	}

	@Test
	void respondsOk() throws Exception {
		HttpStatusHandler handler = new HttpStatusHandler();
		handler.handle(this.request, this.response);
		assertThat(this.servletResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void respondsWithStatus() throws Exception {
		HttpStatusHandler handler = new HttpStatusHandler(HttpStatus.EXPECTATION_FAILED);
		handler.handle(this.request, this.response);
		assertThat(this.servletResponse.getStatus()).isEqualTo(417);
	}

}
