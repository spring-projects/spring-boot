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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LoggingCustomizer}.
 *
 * @author Mark Hobson
 */
public class LoggingCustomizerTest {

	private LoggingCustomizer loggingCustomizer;

	private RestTemplate restTemplate;

	@Mock
	private ClientHttpRequestFactory requestFactory;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		Log log = mock(Log.class);
		given(log.isDebugEnabled()).willReturn(true);
		this.loggingCustomizer = new LoggingCustomizer(log);

		this.restTemplate = new RestTemplate();
		this.restTemplate.setRequestFactory(this.requestFactory);
	}

	@Test
	public void customizeShouldBufferResponses() throws IOException {
		MockClientHttpRequest request = new MockClientHttpRequest();
		MockClientHttpResponse response = new MockClientHttpResponse(
				singleUseStream("hello".getBytes()), HttpStatus.OK);
		request.setResponse(response);
		given(this.requestFactory.createRequest(URI.create("http://example.com"), HttpMethod.GET))
				.willReturn(request);

		this.loggingCustomizer.customize(this.restTemplate);

		ClientHttpResponse actualResponse = this.restTemplate.getRequestFactory()
				.createRequest(URI.create("http://example.com"), HttpMethod.GET)
				.execute();
		assertThat(actualResponse.getBody())
				.hasSameContentAs(new ByteArrayInputStream("hello".getBytes()));
	}

	@Test
	public void customizeShouldAddLoggingInterceptor() {
		RestTemplate restTemplate = new RestTemplate();

		this.loggingCustomizer.customize(restTemplate);

		assertThat(restTemplate.getInterceptors())
				.hasAtLeastOneElementOfType(LoggingInterceptor.class);
	}

	private static InputStream singleUseStream(byte[] bytes) {
		return new InputStream() {
			private int index = 0;

			@Override
			public int read() {
				return this.index < bytes.length ? bytes[this.index++] : -1;
			}
		};
	}
}
