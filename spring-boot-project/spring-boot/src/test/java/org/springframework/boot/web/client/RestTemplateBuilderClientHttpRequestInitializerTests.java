/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTemplateBuilderClientHttpRequestInitializer}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 */
public class RestTemplateBuilderClientHttpRequestInitializerTests {

	private final MockClientHttpRequest request = new MockClientHttpRequest();

	@Test
	void createRequestWhenHasBasicAuthAndNoAuthHeaderAddsHeader() {
		new RestTemplateBuilderClientHttpRequestInitializer(new BasicAuthentication("spring", "boot", null),
				Collections.emptyMap(), Collections.emptySet()).initialize(this.request);
		assertThat(this.request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void createRequestWhenHasBasicAuthAndExistingAuthHeaderDoesNotAddHeader() {
		this.request.getHeaders().setBasicAuth("boot", "spring");
		new RestTemplateBuilderClientHttpRequestInitializer(new BasicAuthentication("spring", "boot", null),
				Collections.emptyMap(), Collections.emptySet()).initialize(this.request);
		assertThat(this.request.getHeaders().get(HttpHeaders.AUTHORIZATION)).doesNotContain("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void createRequestWhenHasDefaultHeadersAddsMissing() {
		this.request.getHeaders().add("one", "existing");
		Map<String, List<String>> defaultHeaders = new LinkedHashMap<>();
		defaultHeaders.put("one", Collections.singletonList("1"));
		defaultHeaders.put("two", Arrays.asList("2", "3"));
		defaultHeaders.put("three", Collections.singletonList("4"));
		new RestTemplateBuilderClientHttpRequestInitializer(null, defaultHeaders, Collections.emptySet())
				.initialize(this.request);
		assertThat(this.request.getHeaders().get("one")).containsExactly("existing");
		assertThat(this.request.getHeaders().get("two")).containsExactly("2", "3");
		assertThat(this.request.getHeaders().get("three")).containsExactly("4");
	}

	@Test
	@SuppressWarnings("unchecked")
	void createRequestWhenHasRequestCustomizersAppliesThemInOrder() {
		Set<RestTemplateRequestCustomizer<?>> customizers = new LinkedHashSet<>();
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		customizers.add(mock(RestTemplateRequestCustomizer.class));
		new RestTemplateBuilderClientHttpRequestInitializer(null, Collections.emptyMap(), customizers)
				.initialize(this.request);
		InOrder inOrder = inOrder(customizers.toArray());
		for (RestTemplateRequestCustomizer<?> customizer : customizers) {
			inOrder.verify((RestTemplateRequestCustomizer<ClientHttpRequest>) customizer).customize(this.request);
		}
	}

}
