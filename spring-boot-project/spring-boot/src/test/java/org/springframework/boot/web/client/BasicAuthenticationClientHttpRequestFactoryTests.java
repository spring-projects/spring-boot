/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BasicAuthenticationClientHttpRequestFactory}.
 *
 * @author Dmytro Nosan
 */
public class BasicAuthenticationClientHttpRequestFactoryTests {

	private final HttpHeaders headers = new HttpHeaders();

	private final BasicAuthentication authentication = new BasicAuthentication("spring", "boot", null);

	private ClientHttpRequestFactory requestFactory;

	@Before
	public void setUp() throws IOException {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(requestFactory.createRequest(any(), any())).willReturn(request);
		given(request.getHeaders()).willReturn(this.headers);
		this.requestFactory = new BasicAuthenticationClientHttpRequestFactory(this.authentication, requestFactory);
	}

	@Test
	public void shouldAddAuthorizationHeader() throws IOException {
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	public void shouldNotAddAuthorizationHeaderAuthorizationAlreadySet() throws IOException {
		this.headers.setBasicAuth("boot", "spring");
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).doesNotContain("Basic c3ByaW5nOmJvb3Q=");

	}

	private ClientHttpRequest createRequest() throws IOException {
		return this.requestFactory.createRequest(URI.create("https://localhost:8080"), HttpMethod.POST);
	}

}
