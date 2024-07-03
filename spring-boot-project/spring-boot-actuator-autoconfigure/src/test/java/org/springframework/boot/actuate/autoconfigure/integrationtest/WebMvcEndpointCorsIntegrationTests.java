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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.assertj.core.api.ThrowingConsumer;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

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
		this.contextRunner.run(withMockMvc((mvc) -> assertThat(mvc.options()
			.uri("/actuator/beans")
			.header("Origin", "foo.example.com")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)));
	}

	@Test
	void settingAllowedOriginsEnablesCors() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mvc) -> {
				assertThat(mvc.options()
					.uri("/actuator/beans")
					.header("Origin", "bar.example.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")).hasStatus(HttpStatus.FORBIDDEN);
				performAcceptedCorsRequest(mvc);
			}));
	}

	@Test
	void settingAllowedOriginPatternsEnablesCors() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origin-patterns:*.example.com",
					"management.endpoints.web.cors.allow-credentials:true")
			.run(withMockMvc((mvc) -> {
				assertThat(mvc.options()
					.uri("/actuator/beans")
					.header("Origin", "bar.example.org")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")).hasStatus(HttpStatus.FORBIDDEN);
				performAcceptedCorsRequest(mvc);
			}));
	}

	@Test
	void maxAgeDefaultsTo30Minutes() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mvc) -> {
				MvcTestResult result = performAcceptedCorsRequest(mvc);
				assertThat(result).hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800");
			}));
	}

	@Test
	void maxAgeCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.max-age: 2400")
			.run(withMockMvc((mvc) -> {
				MvcTestResult result = performAcceptedCorsRequest(mvc);
				assertThat(result).hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "2400");
			}));
	}

	@Test
	void requestsWithDisallowedHeadersAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mvc) -> assertThat(mvc.options()
				.uri("/actuator/beans")
				.header("Origin", "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha")).hasStatus(HttpStatus.FORBIDDEN)));
	}

	@Test
	void allowedHeadersCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allowed-headers:Alpha,Bravo")
			.run(withMockMvc((mvc) -> assertThat(mvc.options()
				.uri("/actuator/beans")
				.header("Origin", "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha")).hasStatusOk()
				.headers()
				.hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Alpha")));
	}

	@Test
	void requestsWithDisallowedMethodsAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com")
			.run(withMockMvc((mvc) -> assertThat(mvc.options()
				.uri("/actuator/beans")
				.header(HttpHeaders.ORIGIN, "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")).hasStatus(HttpStatus.FORBIDDEN)));
	}

	@Test
	void allowedMethodsCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allowed-methods:GET,HEAD")
			.run(withMockMvc((mvc) -> assertThat(mvc.options()
				.uri("/actuator/beans")
				.header(HttpHeaders.ORIGIN, "foo.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "HEAD")).hasStatusOk()
				.headers()
				.hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD")));
	}

	@Test
	void credentialsCanBeAllowed() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allow-credentials:true")
			.run(withMockMvc((mvc) -> {
				MvcTestResult result = performAcceptedCorsRequest(mvc);
				assertThat(result).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
			}));
	}

	@Test
	void credentialsCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.cors.allowed-origins:foo.example.com",
					"management.endpoints.web.cors.allow-credentials:false")
			.run(withMockMvc((mvc) -> {
				MvcTestResult result = performAcceptedCorsRequest(mvc);
				assertThat(result).doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
			}));
	}

	private ContextConsumer<WebApplicationContext> withMockMvc(ThrowingConsumer<MockMvcTester> mvc) {
		return (context) -> mvc.accept(MockMvcTester.from(context));
	}

	private MvcTestResult performAcceptedCorsRequest(MockMvcTester mvc) {
		return performAcceptedCorsRequest(mvc, "/actuator/beans");
	}

	private MvcTestResult performAcceptedCorsRequest(MockMvcTester mvc, String url) {
		MvcTestResult result = mvc.options()
			.uri(url)
			.header(HttpHeaders.ORIGIN, "foo.example.com")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.exchange();
		assertThat(result).hasStatusOk().hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "foo.example.com");
		return result;
	}

}
