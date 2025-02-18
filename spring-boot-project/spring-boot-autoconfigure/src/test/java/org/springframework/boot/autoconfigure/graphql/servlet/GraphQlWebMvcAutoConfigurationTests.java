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

package org.springframework.boot.autoconfigure.graphql.servlet;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import graphql.schema.idl.TypeRuntimeWiring;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlTestDataFetchers;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.graphql.server.webmvc.GraphQlSseHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests for {@link GraphQlWebMvcAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlWebMvcAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
				WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class, CustomWebInterceptor.class)
		.withPropertyValues("spring.main.web-application-type=servlet", "spring.graphql.graphiql.enabled=true",
				"spring.graphql.schema.printer.enabled=true", "spring.graphql.cors.allowed-origins=https://example.com",
				"spring.graphql.cors.allowed-methods=POST", "spring.graphql.cors.allow-credentials=true");

	@Test
	void shouldContributeDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GraphQlHttpHandler.class)
			.hasSingleBean(WebGraphQlHandler.class)
			.doesNotHaveBean(GraphQlWebSocketHandler.class));
	}

	@Test
	void shouldConfigureSseTimeout() {
		this.contextRunner.withPropertyValues("spring.graphql.sse.timeout=10s").run((context) -> {
			assertThat(context).hasSingleBean(GraphQlSseHandler.class);
			GraphQlSseHandler handler = context.getBean(GraphQlSseHandler.class);
			assertThat(handler).hasFieldOrPropertyWithValue("timeout", Duration.ofSeconds(10));
		});
	}

	@Test
	void simpleQueryShouldWork() {
		withMockMvc((mvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			assertThat(mvc.post().uri("/graphql").content("{\"query\": \"" + query + "\"}")).satisfies((result) -> {
				assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_GRAPHQL_RESPONSE);
				assertThat(result).bodyJson()
					.extractingPath("data.bookById.name")
					.asString()
					.isEqualTo("GraphQL for beginners");
			});
		});
	}

	@Test
	void SseSubscriptionShouldWork() {
		withMockMvc((mvc) -> {
			String query = "{ booksOnSale(minPages: 50){ id name pageCount author } }";
			assertThat(mvc.post()
				.uri("/graphql")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.content("{\"query\": \"subscription TestSubscription " + query + "\"}")).satisfies((result) -> {
					assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
					assertThat(result).bodyText()
						.containsSubsequence("event:next",
								"data:{\"data\":{\"booksOnSale\":{\"id\":\"book-1\",\"name\":\"GraphQL for beginners\",\"pageCount\":100,\"author\":\"John GraphQL\"}}}",
								"event:next",
								"data:{\"data\":{\"booksOnSale\":{\"id\":\"book-2\",\"name\":\"Harry Potter and the Philosopher's Stone\",\"pageCount\":223,\"author\":\"Joanne Rowling\"}}}");
				});
		});
	}

	@Test
	void unsupportedContentTypeShouldBeRejected() {
		withMockMvc((mvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			assertThat(mvc.post()
				.uri("/graphql")
				.content("{\"query\": \"" + query + "\"}")
				.contentType(MediaType.TEXT_PLAIN)).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.headers()
				.hasValue("Accept", "application/json");
		});
	}

	@Test
	void httpGetQueryShouldBeRejected() {
		withMockMvc((mvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			assertThat(mvc.get().uri("/graphql?query={query}", "{\"query\": \"" + query + "\"}"))
				.hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
				.headers()
				.hasValue("Allow", "POST");
		});
	}

	@Test
	void shouldRejectMissingQuery() {
		withMockMvc((mvc) -> assertThat(mvc.post().uri("/graphql").content("{}")).hasStatus(HttpStatus.BAD_REQUEST));
	}

	@Test
	void shouldRejectQueryWithInvalidJson() {
		withMockMvc((mvc) -> assertThat(mvc.post().uri("/graphql").content(":)")).hasStatus(HttpStatus.BAD_REQUEST));
	}

	@Test
	void shouldConfigureWebInterceptors() {
		withMockMvc((mvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			assertThat(mvc.post().uri("/graphql").content("{\"query\": \"" + query + "\"}")).hasStatusOk()
				.headers()
				.hasValue("X-Custom-Header", "42");
		});
	}

	@Test
	void shouldExposeSchemaEndpoint() {
		withMockMvc((mvc) -> assertThat(mvc.get().uri("/graphql/schema")).hasStatusOk()
			.hasContentType(MediaType.TEXT_PLAIN)
			.bodyText()
			.contains("type Book"));
	}

	@Test
	void shouldExposeGraphiqlEndpoint() {
		withMockMvc((mvc) -> {
			assertThat(mvc.get().uri("/graphiql")).hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/graphiql?path=/graphql");
			assertThat(mvc.get().uri("/graphiql?path=/graphql")).hasStatusOk()
				.contentType()
				.isEqualTo(MediaType.TEXT_HTML);
		});
	}

	@Test
	void shouldSupportCors() {
		withMockMvc((mvc) -> {
			String query = "{" + "  bookById(id: \\\"book-1\\\"){ " + "    id" + "    name" + "    pageCount"
					+ "    author" + "  }" + "}";
			assertThat(mvc.post()
				.uri("/graphql")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ORIGIN, "https://example.com")
				.content("{\"query\": \"" + query + "\"}"))
				.satisfies((result) -> assertThat(result).hasStatusOk()
					.headers()
					.containsEntry(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, List.of("https://example.com"))
					.containsEntry(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, List.of("true")));
		});
	}

	@Test
	void shouldConfigureWebSocketBeans() {
		this.contextRunner.withPropertyValues("spring.graphql.websocket.path=/ws").run((context) -> {
			assertThat(context).hasSingleBean(GraphQlWebSocketHandler.class);
			assertThat(context.getBeanProvider(HandlerMapping.class).orderedStream().toList()).containsSubsequence(
					context.getBean(WebSocketHandlerMapping.class), context.getBean(RouterFunctionMapping.class),
					context.getBean(RequestMappingHandlerMapping.class));
		});
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
		new GraphQlWebMvcAutoConfiguration.GraphiQlResourceHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("graphiql/index.html")).accepts(hints);
	}

	private void withMockMvc(ThrowingConsumer<MockMvcTester> mvc) {
		this.contextRunner.run((context) -> {
			MockMvcTester mockMVc = MockMvcTester.from(context,
					(builder) -> builder
						.defaultRequest(post("/graphql").contentType(MediaType.APPLICATION_JSON)
							.accept(MediaType.APPLICATION_GRAPHQL_RESPONSE))
						.build());
			mvc.accept(mockMVc);
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
			return (r) -> null;
		}

		@Bean
		@Order(1)
		RouterFunction<?> after() {
			return (r) -> null;
		}

	}

}
