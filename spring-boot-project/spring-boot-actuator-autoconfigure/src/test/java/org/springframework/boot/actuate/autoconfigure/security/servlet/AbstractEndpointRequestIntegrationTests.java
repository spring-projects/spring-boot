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
package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Abstract base class for {@link EndpointRequest} tests.
 *
 * @author Madhura Bhave
 */
public abstract class AbstractEndpointRequestIntegrationTests {

	protected abstract WebApplicationContextRunner getContextRunner();

	@Test
	public void toEndpointShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	public void toAllEndpointsShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e2").exchange().expectStatus().isUnauthorized();
			webTestClient.get().uri("/actuator/e2").header("Authorization", getBasicAuth()).exchange().expectStatus()
					.isOk();
		});
	}

	@Test
	public void toLinksShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator").exchange().expectStatus().isOk();
			webTestClient.get().uri("/actuator/").exchange().expectStatus().isOk();
		});
	}

	protected WebTestClient getWebTestClient(AssertableWebApplicationContext context) {
		int port = context.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
				.getWebServer().getPort();
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public TestEndpoint1 endpoint1() {
			return new TestEndpoint1();
		}

		@Bean
		public TestEndpoint2 endpoint2() {
			return new TestEndpoint2();
		}

		@Bean
		public TestEndpoint3 endpoint3() {
			return new TestEndpoint3();
		}

		@Bean
		public PathMappedEndpoints pathMappedEndpoints() {
			List<ExposableEndpoint<?>> endpoints = new ArrayList<>();
			endpoints.add(mockEndpoint("e1"));
			endpoints.add(mockEndpoint("e2"));
			endpoints.add(mockEndpoint("e3"));
			return new PathMappedEndpoints("/actuator", () -> endpoints);
		}

		private TestPathMappedEndpoint mockEndpoint(String id) {
			TestPathMappedEndpoint endpoint = mock(TestPathMappedEndpoint.class);
			given(endpoint.getEndpointId()).willReturn(EndpointId.of(id));
			given(endpoint.getRootPath()).willReturn(id);
			return endpoint;
		}

	}

	@Endpoint(id = "e1")
	static class TestEndpoint1 {

		@ReadOperation
		public Object getAll() {
			return "endpoint 1";
		}

	}

	@Endpoint(id = "e2")
	static class TestEndpoint2 {

		@ReadOperation
		public Object getAll() {
			return "endpoint 2";
		}

	}

	@Endpoint(id = "e3")
	static class TestEndpoint3 {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	public interface TestPathMappedEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	}

	@Configuration
	static class SecurityConfiguration {

		@Bean
		public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
			return new WebSecurityConfigurerAdapter() {
				@Override
				protected void configure(HttpSecurity http) throws Exception {
					http.authorizeRequests().requestMatchers(EndpointRequest.toLinks()).permitAll()
							.requestMatchers(EndpointRequest.to(TestEndpoint1.class)).permitAll()
							.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated().anyRequest()
							.hasRole("ADMIN").and().httpBasic();
				}
			};
		}

	}

}
