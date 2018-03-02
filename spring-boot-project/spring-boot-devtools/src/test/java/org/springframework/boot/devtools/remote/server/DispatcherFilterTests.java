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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link DispatcherFilter}.
 *
 * @author Phillip Webb
 */
public class DispatcherFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private Dispatcher dispatcher;

	@Mock
	private FilterChain chain;

	@Captor
	private ArgumentCaptor<ServerHttpResponse> serverResponseCaptor;

	@Captor
	private ArgumentCaptor<ServerHttpRequest> serverRequestCaptor;

	private DispatcherFilter filter;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.filter = new DispatcherFilter(this.dispatcher);
	}

	@Test
	public void dispatcherMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Dispatcher must not be null");
		new DispatcherFilter(null);
	}

	@Test
	public void ignoresNotServletRequests() throws Exception {
		ServletRequest request = mock(ServletRequest.class);
		ServletResponse response = mock(ServletResponse.class);
		this.filter.doFilter(request, response, this.chain);
		verifyZeroInteractions(this.dispatcher);
		verify(this.chain).doFilter(request, response);
	}

	@Test
	public void ignoredByDispatcher() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
		HttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, this.chain);
		verify(this.chain).doFilter(request, response);
	}

	@Test
	public void handledByDispatcher() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
		HttpServletResponse response = new MockHttpServletResponse();
		willReturn(true).given(this.dispatcher).handle(any(ServerHttpRequest.class),
				any(ServerHttpResponse.class));
		this.filter.doFilter(request, response, this.chain);
		verifyZeroInteractions(this.chain);
		verify(this.dispatcher).handle(this.serverRequestCaptor.capture(),
				this.serverResponseCaptor.capture());
		ServerHttpRequest dispatcherRequest = this.serverRequestCaptor.getValue();
		ServletServerHttpRequest actualRequest = (ServletServerHttpRequest) dispatcherRequest;
		ServerHttpResponse dispatcherResponse = this.serverResponseCaptor.getValue();
		ServletServerHttpResponse actualResponse = (ServletServerHttpResponse) dispatcherResponse;
		assertThat(actualRequest.getServletRequest()).isEqualTo(request);
		assertThat(actualResponse.getServletResponse()).isEqualTo(response);
	}

}
