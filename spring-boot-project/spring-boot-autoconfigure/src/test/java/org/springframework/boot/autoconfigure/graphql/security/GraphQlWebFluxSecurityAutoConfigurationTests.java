/*
 * Copyright 2020-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql.security;

import java.util.Collections;
import java.util.function.Consumer;

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.Book;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlTestDataFetchers;
import org.springframework.boot.autoconfigure.graphql.reactive.GraphQlWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.ReactiveSecurityDataFetcherExceptionResolver;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for {@link GraphQlWebFluxSecurityAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlWebFluxSecurityAutoConfigurationTests {

	private static final String BASE_URL = "https://spring.example.org/graphql";

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
				CodecsAutoConfiguration.class, JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class,
				GraphQlWebFluxAutoConfiguration.class, GraphQlWebFluxSecurityAutoConfiguration.class,
				ReactiveSecurityAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class, SecurityConfig.class)
		.withPropertyValues("spring.main.web-application-type=reactive");

	@Test
	void contributesExceptionResolver() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ReactiveSecurityDataFetcherExceptionResolver.class));
	}

	@Test
	void anonymousUserShouldBeUnauthorized() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			client.post()
				.uri("")
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("data.bookById.name")
				.doesNotExist()
				.jsonPath("errors[0].extensions.classification")
				.isEqualTo(ErrorType.UNAUTHORIZED.toString());
		});
	}

	@Test
	void authenticatedUserShouldGetData() {
		testWithWebClient((client) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			client.post()
				.uri("")
				.headers((headers) -> headers.setBasicAuth("rob", "rob"))
				.bodyValue("{  \"query\": \"" + query + "\"}")
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("data.bookById.name")
				.isEqualTo("GraphQL for beginners")
				.jsonPath("errors[0].extensions.classification")
				.doesNotExist();
		});
	}

	private void testWithWebClient(Consumer<WebTestClient> consumer) {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
				.configureClient()
				.defaultHeaders((headers) -> {
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
				})
				.baseUrl(BASE_URL)
				.build();
			consumer.accept(client);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataFetchersConfiguration {

		@Bean
		RuntimeWiringConfigurer bookDataFetcher(BookService bookService) {
			return (builder) -> builder.type(TypeRuntimeWiring.newTypeWiring("Query")
				.dataFetcher("bookById", (env) -> bookService.getBookdById(env.getArgument("id"))));
		}

		@Bean
		BookService bookService() {
			return new BookService();
		}

	}

	static class BookService {

		@PreAuthorize("hasRole('USER')")
		@Nullable
		Mono<Book> getBookdById(String id) {
			return Mono.justOrEmpty(GraphQlTestDataFetchers.getBookById(id));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebFluxSecurity
	@EnableReactiveMethodSecurity
	static class SecurityConfig {

		@Bean
		SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
			return http.csrf((spec) -> spec.disable())
				// Demonstrate that method security works
				// Best practice to use both for defense in depth
				.authorizeExchange((requests) -> requests.anyExchange().permitAll())
				.httpBasic(withDefaults())
				.build();
		}

		@Bean
		@SuppressWarnings("deprecation")
		MapReactiveUserDetailsService userDetailsService() {
			User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
			UserDetails rob = userBuilder.username("rob").password("rob").roles("USER").build();
			UserDetails admin = userBuilder.username("admin").password("admin").roles("USER", "ADMIN").build();
			return new MapReactiveUserDetailsService(rob, admin);
		}

	}

}
