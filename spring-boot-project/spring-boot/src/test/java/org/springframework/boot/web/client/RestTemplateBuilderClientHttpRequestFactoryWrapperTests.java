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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTemplateBuilderClientHttpRequestFactoryWrapper}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 */
public class RestTemplateBuilderClientHttpRequestFactoryWrapperTests {

	private ClientHttpRequestFactory requestFactory;

	private final HttpHeaders headers = new HttpHeaders();

	@BeforeEach
	public void setUp() throws IOException {
		this.requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(this.requestFactory.createRequest(any(), any())).willReturn(request);
		given(request.getHeaders()).willReturn(this.headers);
	}

	@Test
	void createRequestWhenHasBasicAuthAndNoAuthHeaderAddsHeader() throws IOException {
		this.requestFactory = new RestTemplateBuilderClientHttpRequestFactoryWrapper(this.requestFactory,
				new BasicAuthentication("spring", "boot", null), Collections.emptyMap(), Collections.emptySet());
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void createRequestWhenHasBasicAuthAndExistingAuthHeaderDoesNotAddHeader() throws IOException {
		this.headers.setBasicAuth("boot", "spring");
		this.requestFactory = new RestTemplateBuilderClientHttpRequestFactoryWrapper(this.requestFactory,
				new BasicAuthentication("spring", "boot", null), Collections.emptyMap(), Collections.emptySet());
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).doesNotContain("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void createRequestWhenHasDefaultHeadersAddsMissing() throws IOException {
		this.headers.add("one", "existing");
		Map<String, String> defaultHeaders = new LinkedHashMap<>();
		defaultHeaders.put("one", "1");
		defaultHeaders.put("two", "2");
		defaultHeaders.put("three", "3");
		this.requestFactory = new RestTemplateBuilderClientHttpRequestFactoryWrapper(this.requestFactory, null,
				defaultHeaders, Collections.emptySet());
		ClientHttpRequest request = createRequest();
		assertThat(request.getHeaders().get("one")).containsExactly("existing");
		assertThat(request.getHeaders().get("two")).containsExactly("2");
		assertThat(request.getHeaders().get("three")).containsExactly("3");
	}

	@Test
	@SuppressWarnings("unchecked")
	void createRequestWhenHasRequestCustomizersAppliesThemInOrder() throws IOException {
		Set<RestTemplateRequestCustomizer<?>> customizers = new LinkedHashSet<>();
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		this.requestFactory = new RestTemplateBuilderClientHttpRequestFactoryWrapper(this.requestFactory, null,
				Collections.emptyMap(), customizers);
		ClientHttpRequest request = createRequest();
		InOrder inOrder = inOrder(customizers.toArray());
		for (RestTemplateRequestCustomizer<?> customizer : customizers) {
			inOrder.verify((RestTemplateRequestCustomizer<ClientHttpRequest>) customizer).customize(request);
		}
	}

	private ClientHttpRequest createRequest() throws IOException {
		return this.requestFactory.createRequest(URI.create("https://localhost:8080"), HttpMethod.POST);
	}

}
