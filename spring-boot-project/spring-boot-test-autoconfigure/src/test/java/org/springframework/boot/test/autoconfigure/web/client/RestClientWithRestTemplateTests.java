/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for building a {@link RestClient} from a {@link RestTemplate}.
 *
 * @author Scott Frederick
 */
class RestClientWithRestTemplateTests {

	@Test
	void buildUsingRestTemplateUriTemplateHandler() {
		RestTemplate restTemplate = new RestTemplate();
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("https://resttemplate.example.com");
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		Builder builder = RestClient.builder(restTemplate);
		RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
		assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
	}

	@Test
	void buildUsingRestClientBuilderBaseUrl() {
		RestTemplate restTemplate = new RestTemplate();
		Builder builder = RestClient.builder(restTemplate).baseUrl("https://restclient.example.com");
		RestClient client = buildMockedClient(builder, "https://restclient.example.com/test");
		assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
	}

	@Test
	void buildUsingRestTemplateUriTemplateHandlerAndRestClientBuilderBaseUrl() {
		RestTemplate restTemplate = new RestTemplate();
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("https://resttemplate.example.com");
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		Builder builder = RestClient.builder(restTemplate).baseUrl("https://restclient.example.com");
		RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
		assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
	}

	private RestClient buildMockedClient(Builder builder, String url) {
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(url)).andRespond(withSuccess());
		return builder.build();
	}

}
