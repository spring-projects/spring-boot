/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.reactive.WebFluxEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.reactive.ReactiveManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the WebFlux actuator endpoints' CORS support
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @see WebFluxEndpointManagementContextConfiguration
 */
class WebFluxEndpointCorsIntegrationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					ReactiveManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include:*");

	@Test
	void corsIsDisabledByDefault() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.options().uri("/actuator/beans")
				.header("Origin", "spring.example.org").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.exchange().expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)));
	}

	@Test
	void settingAllowedOriginsEnablesCors() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org")
				.run(withWebTestClient((webTestClient) -> {
					webTestClient.options().uri("/actuator/beans").header("Origin", "test.example.org")
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").exchange().expectStatus()
							.isForbidden();
					performAcceptedCorsRequest(webTestClient, "/actuator/beans");
				}));
	}

	@Test
	void maxAgeDefaultsTo30Minutes() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org")
				.run(withWebTestClient((webTestClient) -> performAcceptedCorsRequest(webTestClient, "/actuator/beans")
						.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800")));
	}

	@Test
	void maxAgeCanBeConfigured() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org",
						"management.endpoints.web.cors.max-age: 2400")
				.run(withWebTestClient((webTestClient) -> performAcceptedCorsRequest(webTestClient, "/actuator/beans")
						.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "2400")));
	}

	@Test
	void requestsWithDisallowedHeadersAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org")
				.run(withWebTestClient((webTestClient) -> webTestClient.options().uri("/actuator/beans")
						.header("Origin", "spring.example.org").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha").exchange().expectStatus()
						.isForbidden()));
	}

	@Test
	void allowedHeadersCanBeConfigured() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org",
						"management.endpoints.web.cors.allowed-headers:Alpha,Bravo")
				.run(withWebTestClient((webTestClient) -> webTestClient.options().uri("/actuator/beans")
						.header("Origin", "spring.example.org").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha").exchange().expectStatus().isOk()
						.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Alpha")));
	}

	@Test
	void requestsWithDisallowedMethodsAreRejected() {
		this.contextRunner.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org")
				.run(withWebTestClient((webTestClient) -> webTestClient.options().uri("/actuator/beans")
						.header("Origin", "spring.example.org")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH").exchange().expectStatus()
						.isForbidden()));
	}

	@Test
	void allowedMethodsCanBeConfigured() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org",
						"management.endpoints.web.cors.allowed-methods:GET,HEAD")
				.run(withWebTestClient((webTestClient) -> webTestClient.options().uri("/actuator/beans")
						.header("Origin", "spring.example.org")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "HEAD").exchange().expectStatus().isOk()
						.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD")));
	}

	@Test
	void credentialsCanBeAllowed() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org",
						"management.endpoints.web.cors.allow-credentials:true")
				.run(withWebTestClient((webTestClient) -> performAcceptedCorsRequest(webTestClient, "/actuator/beans")
						.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")));
	}

	@Test
	void credentialsCanBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.cors.allowed-origins:spring.example.org",
						"management.endpoints.web.cors.allow-credentials:false")
				.run(withWebTestClient((webTestClient) -> performAcceptedCorsRequest(webTestClient, "/actuator/beans")
						.expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)));
	}

	private ContextConsumer<ReactiveWebApplicationContext> withWebTestClient(Consumer<WebTestClient> webTestClient) {
		return (context) -> webTestClient.accept(WebTestClient.bindToApplicationContext(context).configureClient()
				.baseUrl("https://spring.example.org").build());
	}

	private WebTestClient.ResponseSpec performAcceptedCorsRequest(WebTestClient webTestClient, String url) {
		return webTestClient.options().uri(url).header(HttpHeaders.ORIGIN, "spring.example.org")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").exchange().expectHeader()
				.valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "spring.example.org").expectStatus().isOk();
	}

}
