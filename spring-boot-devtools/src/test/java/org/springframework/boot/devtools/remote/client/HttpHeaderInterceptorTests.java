/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.remote.client;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.devtools.remote.client.HttpHeaderInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link HttpHeaderInterceptor}.
 *
 * @author Rob Winch
 * @since 1.3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpHeaderInterceptorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private String name;

	private String value;

	private HttpHeaderInterceptor interceptor;

	private HttpRequest request;

	private byte[] body;

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private ClientHttpResponse response;

	private MockHttpServletRequest httpRequest;

	@Before
	public void setup() throws IOException {
		this.body = new byte[] {};
		this.httpRequest = new MockHttpServletRequest();
		this.request = new ServletServerHttpRequest(this.httpRequest);
		this.name = "X-AUTH-TOKEN";
		this.value = "secret";
		given(this.execution.execute(this.request, this.body)).willReturn(this.response);
		this.interceptor = new HttpHeaderInterceptor(this.name, this.value);
	}

	@Test
	public void constructorNullHeaderName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be empty");
		new HttpHeaderInterceptor(null, this.value);
	}

	@Test
	public void constructorEmptyHeaderName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be empty");
		new HttpHeaderInterceptor("", this.value);
	}

	@Test
	public void constructorNullHeaderValue() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		new HttpHeaderInterceptor(this.name, null);
	}

	@Test
	public void constructorEmptyHeaderValue() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		new HttpHeaderInterceptor(this.name, "");
	}

	@Test
	public void intercept() throws IOException {
		ClientHttpResponse result = this.interceptor.intercept(this.request, this.body,
				this.execution);
		assertThat(this.request.getHeaders().getFirst(this.name), equalTo(this.value));
		assertThat(result, equalTo(this.response));
	}

}
