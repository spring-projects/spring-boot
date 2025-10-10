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

package org.springframework.boot.graphql.autoconfigure.reactive;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.GraphQlTestDataFetchers;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.webflux.GraphQlHttpHandler;
import org.springframework.graphql.server.webflux.GraphQlSseHandler;
import org.springframework.graphql.server.webflux.GraphQlWebSocketHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link GraphQlWebFluxAutoConfiguration}
 *
 * @author Brian Clozel
 */
@WithResource(name = "graphql/types/book.graphqls", content = """
		type Book {
		    id: ID
		    name: String
		    pageCount: Int
		    author: String
		}
		""")
@WithResource(name = "graphql/schema.graphqls", content = """
		type Query {
		    greeting(name: String! = "Spring"): String!
		    bookById(id: ID): Book
		    books: BookConnection
		}

		type Subscription {
		    booksOnSale(minPages: Int) : Book!
		}
		""")
class GraphQlWebFluxAutoConfigurationTests {

	private static final String BASE_URL = "https://spring.example.org/";

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
				CodecsAutoConfiguration.class, JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class,
				GraphQlWebFluxAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class, CustomWebInterceptor.class)
		.withPropertyValues("spring.main.web-application-type=reactive", "spring.graphql.graphiql.enabled=true",
				"spring.graphql.schema.printer.enabled=true", "spring.graphql.cors.allowed-origins=https://example.com",
				"spring.graphql.cors.allowed-methods=POST", "spring.graphql.cors.allow-credentials=true");

	@Test
	void shouldContributeDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GraphQlHttpHandler.class)
			.hasSingleBean(WebGraphQlHandler.class)
			.doesNotHaveBean(GraphQlWebSocketHandler.class));
	}

	@Test
	void simpleQueryShouldWork() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			client.post()
				.uri("/graphql")
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.contentType(MediaType.APPLICATION_GRAPHQL_RESPONSE_VALUE)
				.expectBody()
				.jsonPath("data.bookById.name")
				.isEqualTo("GraphQL for beginners");
		});
	}

	@Test
	void SseSubscriptionShouldWork() {
		testWithWebClient((client) -> {
			String query = "{ booksOnSale(minPages: 50){ id name pageCount author } }";
			EntityExchangeResult<String> result = client.post()
				.uri("/graphql")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.bodyValue("{  \"query\": \"subscription TestSubscription " + query + "\"}")
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
				.expectBody(String.class)
				.returnResult();
			assertThat(result.getResponseBody()).contains("event:next",
					"data:{\"data\":{\"booksOnSale\":{\"id\":\"book-1\",\"name\":\"GraphQL for beginners\",\"pageCount\":100,\"author\":\"John GraphQL\"}}}",
					"event:next",
					"data:{\"data\":{\"booksOnSale\":{\"id\":\"book-2\",\"name\":\"Harry Potter and the Philosopher's Stone\",\"pageCount\":223,\"author\":\"Joanne Rowling\"}}}",
					"event:complete");
		});
	}

	@Test
	void unsupportedContentTypeShouldBeRejected() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			client.post()
				.uri("/graphql")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.expectHeader()
				.valueEquals("Accept", "application/json");
		});
	}

	@Test
	void httpGetQueryShouldBeRejected() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			client.get()
				.uri("/graphql?query={query}", "{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
				.expectHeader()
				.valueEquals("Allow", "POST");
		});
	}

	@Test
	void shouldRejectMissingQuery() {
		testWithWebClient(
				(client) -> client.post().uri("/graphql").bodyValue("{}").exchange().expectStatus().isBadRequest());
	}

	@Test
	void shouldRejectQueryWithInvalidJson() {
		testWithWebClient(
				(client) -> client.post().uri("/graphql").bodyValue(":)").exchange().expectStatus().isBadRequest());
	}

	@Test
	void shouldConfigureWebInterceptors() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";

			client.post()
				.uri("/graphql")
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.valueEquals("X-Custom-Header", "42");
		});
	}

	@Test
	void shouldExposeSchemaEndpoint() {
		testWithWebClient((client) -> client.get()
			.uri("/graphql/schema")
			.accept(MediaType.ALL)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.TEXT_PLAIN)
			.expectBody(String.class)
			.value(containsString("type Book")));
	}

	@Test
	void shouldExposeGraphiqlEndpoint() {
		testWithWebClient((client) -> {
			client.get()
				.uri("/graphiql")
				.exchange()
				.expectStatus()
				.is3xxRedirection()
				.expectHeader()
				.location("https://spring.example.org/graphiql?path=/graphql");
			client.get()
				.uri("/graphiql?path=/graphql")
				.accept(MediaType.ALL)
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.contentType(MediaType.TEXT_HTML);
		});
	}

	@Test
	void shouldSupportCors() {
		testWithWebClient((client) -> {
			String query = "{" + "  bookById(id: \\\"book-1\\\"){ " + "    id" + "    name" + "    pageCount"
					+ "    author" + "  }" + "}";
			client.post()
				.uri("/graphql")
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ORIGIN, "https://example.com")
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com")
				.expectHeader()
				.valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		});
	}

	@Test
	void shouldConfigureWebSocketBeans() {
		this.contextRunner.withPropertyValues("spring.graphql.websocket.path=/ws")
			.run((context) -> assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class));
	}

	@Test
	void shouldConfigureWebSocketProperties() {
		this.contextRunner
			.withPropertyValues("spring.graphql.websocket.path=/ws",
					"spring.graphql.websocket.connection-init-timeout=120s", "spring.graphql.websocket.keep-alive=30s")
			.run((context) -> {
				assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class);
				GraphQlWebSocketHandler graphQlWebSocketHandler = context.getBean(GraphQlWebSocketHandler.class);
				assertThat(graphQlWebSocketHandler).extracting("initTimeoutDuration")
					.isEqualTo(Duration.ofSeconds(120));
				assertThat(graphQlWebSocketHandler).extracting("keepAliveDuration").isEqualTo(Duration.ofSeconds(30));
			});
	}

	@Test
	void shouldConfigureSseTimeout() {
		this.contextRunner.withPropertyValues("spring.graphql.http.sse.timeout=10s").run((context) -> {
			assertThat(context).hasSingleBean(GraphQlSseHandler.class);
			GraphQlSseHandler handler = context.getBean(GraphQlSseHandler.class);
			assertThat(handler).hasFieldOrPropertyWithValue("timeout", Duration.ofSeconds(10));
		});
	}

	@Test
	void shouldConfigureSseKeepAlive() {
		this.contextRunner.withPropertyValues("spring.graphql.http.sse.keep-alive=5s").run((context) -> {
			assertThat(context).hasSingleBean(GraphQlSseHandler.class);
			GraphQlSseHandler handler = context.getBean(GraphQlSseHandler.class);
			assertThat(handler).hasFieldOrPropertyWithValue("keepAliveDuration", Duration.ofSeconds(5));
		});
	}

	@Test
	void routerFunctionShouldHaveOrderZero() {
		this.contextRunner.withUserConfiguration(CustomRouterFunctions.class).run((context) -> {
			Map<String, ?> beans = context.getBeansOfType(RouterFunction.class);
			Object[] ordered = context.getBeanProvider(RouterFunction.class).orderedStream().toArray();
			assertThat(beans.get("before")).isSameAs(ordered[0]);
			assertThat(beans.get("graphQlRouterFunction")).isSameAs(ordered[1]);
			assertThat(beans.get("after")).isSameAs(ordered[2]);
		});
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new GraphQlWebFluxAutoConfiguration.GraphiQlResourceHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("graphiql/index.html")).accepts(hints);
	}

	private void testWithWebClient(Consumer<WebTestClient> consumer) {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
				.configureClient()
				.defaultHeaders((headers) -> {
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.setAccept(Collections.singletonList(MediaType.APPLICATION_GRAPHQL_RESPONSE));
				})
				.baseUrl(BASE_URL)
				.build();
			consumer.accept(client);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetchersConfiguration {

		@Bean
		RuntimeWiringConfigurer bookDataFetcher() {
			return (builder) -> {
				builder.type(TypeRuntimeWiring.newTypeWiring("Query")
					.dataFetcher("bookById", GraphQlTestDataFetchers.getBookByIdDataFetcher()));
				builder.type(TypeRuntimeWiring.newTypeWiring("Subscription")
					.dataFetcher("booksOnSale", GraphQlTestDataFetchers.getBooksOnSaleDataFetcher()));
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebInterceptor {

		@Bean
		WebGraphQlInterceptor customWebGraphQlInterceptor() {
			return (webInput, interceptorChain) -> interceptorChain.next(webInput)
				.doOnNext((output) -> output.getResponseHeaders().add("X-Custom-Header", "42"));
		}

	}

	@Configuration
	static class CustomRouterFunctions {

		@Bean
		@Order(-1)
		RouterFunction<?> before() {
			return (r) -> Mono.empty();
		}

		@Bean
		@Order(1)
		RouterFunction<?> after() {
			return (r) -> Mono.empty();
		}

	}

}
