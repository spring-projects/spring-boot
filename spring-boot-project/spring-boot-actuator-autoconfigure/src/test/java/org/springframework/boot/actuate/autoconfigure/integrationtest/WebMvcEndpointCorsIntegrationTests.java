/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the MVC actuator endpoints' CORS support
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @see WebMvcEndpointManagementContextConfiguration
 */
class WebMvcEndpointCorsIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
				ServletManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class))
		.withPropertyValues("management.endpoints.web.exposure.include:*");

	@Test
	void corsIsDisabledByDefault() {
		this.contextRunner.run(withMockMvc((mockMvc) -> mockMvc
			.perform(options("/actuator/beans").header("Origin", "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))));
	}

	@Test
	void settingAllowedOriginsEnablesCors() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mockMvc) -> {
				mockMvc
					.perform(options("/actuator/beans").header("Origin", "bar.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
					.andExpect(status().isForbidden());
				performAcceptedCorsRequest(mockMvc);
			}));
	}

	@Test
	void settingAllowedOriginPatternsEnablesCors() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origin-patterns:*.example.com",
					"management.endpoints.web.cors.allow-credentials:true")
			.run(withMockMvc((mockMvc) -> {
				mockMvc
					.perform(options("/actuator/beans").header("Origin", "bar.example.org")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
					.andExpect(status().isForbidden());
				performAcceptedCorsRequest(mockMvc);
			}));
	}

	@Test
	void maxAgeDefaultsTo30Minutes() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mockMvc) -> performAcceptedCorsRequest(mockMvc)
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"))));
	}

	@Test
	void maxAgeCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.max-age: 2400")
			.run(withMockMvc((mockMvc) -> performAcceptedCorsRequest(mockMvc)
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "2400"))));
	}

	@Test
	void requestsWithDisallowedHeadersAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mockMvc) ->

			mockMvc
				.perform(options("/actuator/beans").header("Origin", "foo.example.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isForbidden())));
	}

	@Test
	void allowedHeadersCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allowed-headers:Alpha,Bravo")
			.run(withMockMvc((mockMvc) -> mockMvc
				.perform(options("/actuator/beans").header("Origin", "foo.example.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Alpha"))));
	}

	@Test
	void requestsWithDisallowedMethodsAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mockMvc) -> mockMvc
				.perform(options("/actuator/beans").header(HttpHeaders.ORIGIN, "foo.example.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
				.andExpect(status().isForbidden())));
	}

	@Test
	void allowedMethodsCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allowed-methods:GET,HEAD")
			.run(withMockMvc((mockMvc) -> mockMvc
				.perform(options("/actuator/beans").header(HttpHeaders.ORIGIN, "foo.example.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "HEAD"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD"))));
	}

	@Test
	void credentialsCanBeAllowed() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allow-credentials:true")
			.run(withMockMvc((mockMvc) -> performAcceptedCorsRequest(mockMvc)
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))));
	}

	@Test
	void credentialsCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allow-credentials:false")
			.run(withMockMvc((mockMvc) -> performAcceptedCorsRequest(mockMvc)
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))));
	}

	private ContextConsumer<WebApplicationContext> withMockMvc(MockMvcConsumer mockMvc) {
		return (context) -> mockMvc.accept(MockMvcBuilders.webAppContextSetup(context).build());
	}

	private ResultActions performAcceptedCorsRequest(MockMvc mockMvc) throws Exception {
		return performAcceptedCorsRequest(mockMvc, "/actuator/beans");
	}

	private ResultActions performAcceptedCorsRequest(MockMvc mockMvc, String url) throws Exception {
		return mockMvc
			.perform(options(url).header(HttpHeaders.ORIGIN, "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "foo.example.com"))
			.andExpect(status().isOk());
	}

	@FunctionalInterface
	interface MockMvcConsumer {

		void accept(MockMvc mockMvc) throws Exception;

	}

}
