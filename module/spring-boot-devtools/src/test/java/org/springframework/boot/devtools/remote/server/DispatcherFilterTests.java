/*
 * Copyright 2012-present the original author or authors.
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DispatcherFilter}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class DispatcherFilterTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	private Dispatcher dispatcher;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private FilterChain chain;

	private DispatcherFilter filter;

	@BeforeEach
	void setup() {
		this.filter = new DispatcherFilter(this.dispatcher);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void dispatcherMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DispatcherFilter(null))
			.withMessageContaining("'dispatcher' must not be null");
	}

	@Test
	void ignoresNotServletRequests() throws Exception {
		ServletRequest request = mock(ServletRequest.class);
		ServletResponse response = mock(ServletResponse.class);
		this.filter.doFilter(request, response, this.chain);
		then(this.dispatcher).shouldHaveNoInteractions();
		then(this.chain).should().doFilter(request, response);
	}

	@Test
	void ignoredByDispatcher() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
		HttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, this.chain);
		then(this.chain).should().doFilter(request, response);
	}

	@Test
	void handledByDispatcher() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
		HttpServletResponse response = new MockHttpServletResponse();
		willReturn(true).given(this.dispatcher).handle(any(ServerHttpRequest.class), any(ServerHttpResponse.class));
		this.filter.doFilter(request, response, this.chain);
		then(this.chain).shouldHaveNoInteractions();
		then(this.dispatcher).should()
			.handle(assertArg((serverHttpRequest) -> assertThat(serverHttpRequest).isInstanceOfSatisfying(
					ServletServerHttpRequest.class,
					(servletServerHttpRequest) -> assertThat(servletServerHttpRequest.getServletRequest())
						.isEqualTo(request))),
					assertArg((serverHttpResponse) -> assertThat(serverHttpResponse).isInstanceOfSatisfying(
							ServletServerHttpResponse.class,
							(servletServerHttpResponse) -> assertThat(servletServerHttpResponse.getServletResponse())
								.isEqualTo(response))));
	}

}
