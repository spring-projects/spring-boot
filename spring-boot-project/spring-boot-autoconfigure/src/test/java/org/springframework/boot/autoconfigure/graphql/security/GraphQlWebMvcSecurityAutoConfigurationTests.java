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

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.Book;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlTestDataFetchers;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link GraphQlWebMvcSecurityAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlWebMvcSecurityAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
				WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class,
				GraphQlWebMvcSecurityAutoConfiguration.class, SecurityAutoConfiguration.class))
		.withUserConfiguration(DataFetchersConfiguration.class, SecurityConfig.class)
		.withPropertyValues("spring.main.web-application-type=servlet");

	@Test
	void contributesSecurityComponents() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(SecurityDataFetcherExceptionResolver.class));
	}

	@Test
	void anonymousUserShouldBeUnauthorized() {
		testWith((mockMvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			MvcResult result = mockMvc.perform(post("/graphql").content("{\"query\": \"" + query + "\"}")).andReturn();
			mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("data.bookById.name").doesNotExist())
				.andExpect(jsonPath("errors[0].extensions.classification").value(ErrorType.UNAUTHORIZED.toString()));
		});
	}

	@Test
	void authenticatedUserShouldGetData() {
		testWith((mockMvc) -> {
			String query = "{  bookById(id: \\\"book-1\\\"){ id name pageCount author }}";
			MvcResult result = mockMvc
				.perform(post("/graphql").content("{\"query\": \"" + query + "\"}").with(user("rob")))
				.andReturn();
			mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("data.bookById.name").value("GraphQL for beginners"))
				.andExpect(jsonPath("errors").doesNotExist());
		});

	}

	private void testWith(MockMvcConsumer mockMvcConsumer) {
		this.contextRunner.run((context) -> {
			MediaType mediaType = MediaType.APPLICATION_JSON;
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.defaultRequest(post("/graphql").contentType(mediaType).accept(mediaType))
				.apply(springSecurity())
				.build();
			mockMvcConsumer.accept(mockMvc);
		});
	}

	private interface MockMvcConsumer {

		void accept(MockMvc mockMvc) throws Exception;

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
		Book getBookdById(String id) {
			return GraphQlTestDataFetchers.getBookById(id);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	@EnableMethodSecurity(prePostEnabled = true)
	@SuppressWarnings("deprecation")
	static class SecurityConfig {

		@Bean
		DefaultSecurityFilterChain springWebFilterChain(HttpSecurity http) throws Exception {
			return http.csrf((c) -> c.disable())
				// Demonstrate that method security works
				// Best practice to use both for defense in depth
				.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll())
				.httpBasic(withDefaults())
				.build();
		}

		@Bean
		InMemoryUserDetailsManager userDetailsService() {
			User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
			UserDetails rob = userBuilder.username("rob").password("rob").roles("USER").build();
			UserDetails admin = userBuilder.username("admin").password("admin").roles("USER", "ADMIN").build();
			return new InMemoryUserDetailsManager(rob, admin);
		}

	}

}
