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
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpHeadersCustomizingClientHttpRequestFactory}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 */
public class HttpHeadersCustomizingClientHttpRequestFactoryTests {

	private final HttpHeaders headers = new HttpHeaders();

	private ClientHttpRequestFactory requestFactory;

	@BeforeEach
	public void setUp() throws IOException {
		this.requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(this.requestFactory.createRequest(any(), any())).willReturn(request);
		given(request.getHeaders()).willReturn(this.headers);
	}

	@Test
	void shouldAddAuthorizationHeader() throws IOException {
		this.requestFactory = new HttpHeadersCustomizingClientHttpRequestFactory(
				Collections.singleton(SimpleHttpHeaderDefaultingCustomizer.basicAuthentication("spring", "boot", null)),
				this.requestFactory);
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void shouldNotAddAuthorizationHeaderAuthorizationAlreadySet() throws IOException {
		this.headers.setBasicAuth("boot", "spring");
		this.requestFactory = new HttpHeadersCustomizingClientHttpRequestFactory(
				Collections.singleton(SimpleHttpHeaderDefaultingCustomizer.basicAuthentication("spring", "boot", null)),
				this.requestFactory);
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).doesNotContain("Basic c3ByaW5nOmJvb3Q=");

	}

	@Test
	void shouldApplyCustomizersInTheProvidedOrder() throws IOException {
		this.requestFactory = new HttpHeadersCustomizingClientHttpRequestFactory(
				Arrays.asList((headers) -> headers.add("foo", "bar"),
						SimpleHttpHeaderDefaultingCustomizer.basicAuthentication("spring", "boot", null),
						SimpleHttpHeaderDefaultingCustomizer.singleHeader(HttpHeaders.AUTHORIZATION, "won't do")),
				this.requestFactory);
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders()).containsOnlyKeys("foo", HttpHeaders.AUTHORIZATION);
		assertThat(request.getHeaders().get("foo")).containsExactly("bar");
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	private ClientHttpRequest createRequest() throws IOException {
		return this.requestFactory.createRequest(URI.create("https://localhost:8080"), HttpMethod.POST);
	}

}
