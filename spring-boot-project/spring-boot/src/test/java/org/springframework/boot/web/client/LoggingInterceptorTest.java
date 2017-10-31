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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingInterceptor}.
 *
 * @author Mark Hobson
 */
public class LoggingInterceptorTest {

	@Mock
	private Log log;

	private LoggingInterceptor loggingInterceptor;

	@Mock
	private ClientHttpRequestExecution execution;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		given(this.log.isDebugEnabled()).willReturn(true);

		this.loggingInterceptor = new LoggingInterceptor(this.log);
	}

	@Test
	public void canLogRequest() throws IOException, URISyntaxException {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
				new URI("/hello"));
		byte[] body = "world".getBytes();
		given(this.execution.execute(request, body))
				.willReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

		this.loggingInterceptor.intercept(request, body, this.execution);

		verify(this.log).debug("Request: POST /hello world");
	}

	@Test
	public void canLogResponse() throws IOException, URISyntaxException {
		MockClientHttpRequest request = new MockClientHttpRequest();
		byte[] body = new byte[0];
		given(this.execution.execute(request, body))
				.willReturn(new MockClientHttpResponse("hello".getBytes(), HttpStatus.OK));

		this.loggingInterceptor.intercept(request, body, this.execution);

		verify(this.log).debug("Response: 200 hello");
	}
}
