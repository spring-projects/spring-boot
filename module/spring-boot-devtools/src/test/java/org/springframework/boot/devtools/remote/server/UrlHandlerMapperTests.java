/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.remote.server;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlHandlerMapper}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class UrlHandlerMapperTests {

	private final Handler handler = mock(Handler.class);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void requestUriMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper(null, this.handler))
			.withMessageContaining("'url' must not be empty");
	}

	@Test
	void requestUriMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper("", this.handler))
			.withMessageContaining("'url' must not be empty");
	}

	@Test
	void requestUrlMustStartWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper("tunnel", this.handler))
			.withMessageContaining("'url' must start with '/'");
	}

	@Test
	void handlesMatchedUrl() {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/tunnel");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isEqualTo(this.handler);
	}

	@Test
	void ignoresDifferentUrl() {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/tunnel/other");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isNull();
	}

}
