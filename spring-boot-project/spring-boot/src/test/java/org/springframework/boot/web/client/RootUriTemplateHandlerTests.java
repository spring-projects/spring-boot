/*
 * Copyright 2012-2018 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RootUriTemplateHandler}.
 *
 * @author Phillip Webb
 */
public class RootUriTemplateHandlerTests {

	private URI uri;

	@Mock
	public UriTemplateHandler delegate;

	public UriTemplateHandler handler;

	@Before
	@SuppressWarnings("unchecked")
	public void setup() throws URISyntaxException {
		MockitoAnnotations.initMocks(this);
		this.uri = new URI("http://example.com/hello");
		this.handler = new RootUriTemplateHandler("http://example.com", this.delegate);
		given(this.delegate.expand(anyString(), any(Map.class))).willReturn(this.uri);
		given(this.delegate.expand(anyString(), any(Object[].class)))
				.willReturn(this.uri);
	}

	@Test
	public void createWithNullRootUriShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RootUriTemplateHandler((String) null))
				.withMessageContaining("RootUri must not be null");
	}

	@Test
	public void createWithNullHandlerShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RootUriTemplateHandler("http://example.com", null))
				.withMessageContaining("Handler must not be null");
	}

	@Test
	public void expandMapVariablesShouldPrefixRoot() {
		HashMap<String, Object> uriVariables = new HashMap<>();
		URI expanded = this.handler.expand("/hello", uriVariables);
		verify(this.delegate).expand("http://example.com/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	public void expandMapVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
		HashMap<String, Object> uriVariables = new HashMap<>();
		URI expanded = this.handler.expand("http://spring.io/hello", uriVariables);
		verify(this.delegate).expand("http://spring.io/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	public void expandArrayVariablesShouldPrefixRoot() {
		Object[] uriVariables = new Object[0];
		URI expanded = this.handler.expand("/hello", uriVariables);
		verify(this.delegate).expand("http://example.com/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	public void expandArrayVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
		Object[] uriVariables = new Object[0];
		URI expanded = this.handler.expand("http://spring.io/hello", uriVariables);
		verify(this.delegate).expand("http://spring.io/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	public void applyShouldWrapExistingTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(this.delegate);
		this.handler = RootUriTemplateHandler.addTo(restTemplate, "http://example.com");
		Object[] uriVariables = new Object[0];
		URI expanded = this.handler.expand("/hello", uriVariables);
		verify(this.delegate).expand("http://example.com/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

}
