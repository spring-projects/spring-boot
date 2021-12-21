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

package org.springframework.boot.autoconfigure.graphql.servlet;

import graphql.schema.idl.TypeRuntimeWiring;
import org.junit.jupiter.api.Test;

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
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link GraphQlWebMvcAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlWebMvcAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
							HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
							GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class))
			.withUserConfiguration(DataFetchersConfiguration.class, CustomWebInterceptor.class)
			.withPropertyValues("spring.main.web-application-type=servlet");

	@Test
	void simpleQueryShouldWork() {
		testWith((mockMvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			MvcResult result = mockMvc.perform(post("/graphql").content("{\"query\": \"" + query + "\"}")).andReturn();
			mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("data.bookById.name").value("GraphQL for beginners"));
		});
	}

	@Test
	void httpGetQueryShouldBeSupported() {
		testWith((mockMvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			mockMvc.perform(get("/graphql?query={query}", "{\"query\": \"" + query + "\"}"))
					.andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", "POST"));
		});
	}

	@Test
	void shouldRejectMissingQuery() {
		testWith((mockMvc) -> mockMvc.perform(post("/graphql").content("{}")).andExpect(status().isBadRequest()));
	}

	@Test
	void shouldRejectQueryWithInvalidJson() {
		testWith((mockMvc) -> mockMvc.perform(post("/graphql").content(":)")).andExpect(status().isBadRequest()));
	}

	@Test
	void shouldConfigureWebInterceptors() {
		testWith((mockMvc) -> {
			String query = "{ bookById(id: \\\"book-1\\\"){ id name pageCount author } }";
			MvcResult result = mockMvc.perform(post("/graphql").content("{\"query\": \"" + query + "\"}")).andReturn();
			mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk())
					.andExpect(header().string("X-Custom-Header", "42"));
		});
	}

	private void testWith(MockMvcConsumer mockMvcConsumer) {
		this.contextRunner.run((context) -> {
			MediaType mediaType = MediaType.APPLICATION_JSON;
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
					.defaultRequest(post("/graphql").contentType(mediaType).accept(mediaType)).build();
			mockMvcConsumer.accept(mockMvc);
		});
	}

	private interface MockMvcConsumer {

		void accept(MockMvc mockMvc) throws Exception;

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
