/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webclient.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example web client using {@code WebClient} with {@link WebClientTest @WebClientTest}
 * tests.
 *
 * @author Scott Frederick
 */
@Service
public class ExampleWebClientService {

	private final WebClient.Builder builder;

	private final WebClient webClient;

	public ExampleWebClientService(WebClient.Builder builder) {
		this.builder = builder;
		this.webClient = builder.build();
	}

	protected WebClient.Builder getWebClientBuilder() {
		return this.builder;
	}

	public @Nullable String test() {
		ResponseEntity<String> response = this.webClient.get().uri("/test").retrieve().toEntity(String.class).block();
		assertThat(response).isNotNull();
		return response.getBody();
	}

	public void testPostWithBody(String body) {
		this.webClient.post().uri("/test").bodyValue(body).retrieve().toBodilessEntity().block();
	}

}
