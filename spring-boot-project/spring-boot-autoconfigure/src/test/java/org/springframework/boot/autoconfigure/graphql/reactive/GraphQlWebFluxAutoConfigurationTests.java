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

package org.springframework.boot.autoconfigure.graphql.reactive;

import java.util.Collections;
import java.util.function.Consumer;

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlTestDataFetchers;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link GraphQlWebFluxAutoConfiguration}
 *
 * @author Brian Clozel
 */
class GraphQlWebFluxAutoConfigurationTests {

	private static final String BASE_URL = "https://spring.example.org/graphql";

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
					CodecsAutoConfiguration.class, JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class,
					GraphQlWebFluxAutoConfiguration.class))
			.withUserConfiguration(DataFetchersConfiguration.class, CustomWebInterceptor.class).withPropertyValues(
					"spring.main.web-application-type=reactive", "spring.graphql.schema.printer.enabled=true");

	@Test
	void simpleQueryShouldWork() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			client.post().uri("").bodyValue("{  \"query\": \"" + query + "\"}").exchange().expectStatus().isOk()
					.expectBody().jsonPath("data.bookById.name").isEqualTo("GraphQL for beginners");
		});
	}

	@Test
	void httpGetQueryShouldBeSupported() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			client.get().uri("?query={query}", "{  \"query\": \"" + query + "\"}").exchange().expectStatus()
					.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED).expectHeader().valueEquals("Allow", "POST");
		});
	}

	@Test
	void shouldRejectMissingQuery() {
		testWithWebClient((client) -> client.post().uri("").bodyValue("{}").exchange().expectStatus().isBadRequest());
	}

	@Test
	void shouldRejectQueryWithInvalidJson() {
		testWithWebClient((client) -> client.post().uri("").bodyValue(":)").exchange().expectStatus().isBadRequest());
	}

	@Test
	void shouldConfigureWebInterceptors() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";

			client.post().uri("").bodyValue("{  \"query\": \"" + query + "\"}").exchange().expectStatus().isOk()
					.expectHeader().valueEquals("X-Custom-Header", "42");
		});
	}

	@Test
	void shouldExposeSchemaEndpoint() {
		testWithWebClient((client) -> client.get().uri("/schema").accept(MediaType.ALL).exchange()
				.expectStatus().isOk().expectHeader().contentType(MediaType.TEXT_PLAIN).expectBody(String.class)
				.value(containsString("type Book")));
	}

	private void testWithWebClient(Consumer<WebTestClient> consumer) {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context).configureClient()
					.defaultHeaders((headers) -> {
						headers.setContentType(MediaType.APPLICATION_JSON);
						headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
					}).baseUrl(BASE_URL).build();
			consumer.accept(client);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetchersConfiguration {

		@Bean
		RuntimeWiringConfigurer bookDataFetcher() {
			return (builder) -> builder.type(TypeRuntimeWiring.newTypeWiring("Query").dataFetcher("bookById",
					GraphQlTestDataFetchers.getBookByIdDataFetcher()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebInterceptor {

		@Bean
		WebInterceptor customWebInterceptor() {
			return (webInput, interceptorChain) -> interceptorChain.next(webInput)
					.map((output) -> output.transform((builder) -> builder.responseHeader("X-Custom-Header", "42")));
		}

	}

}
