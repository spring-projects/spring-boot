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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Abstract base class for {@link EndpointRequest} tests.
 *
 * @author Madhura Bhave
 */
abstract class AbstractEndpointRequestIntegrationTests {

	@Test
	void toEndpointShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toAllEndpointsShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e2").exchange().expectStatus().isUnauthorized();
			webTestClient.get()
				.uri("/actuator/e2")
				.header("Authorization", getBasicAuth())
				.exchange()
				.expectStatus()
				.isOk();
		});
	}

	@Test
	void toLinksShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator").exchange().expectStatus().isOk();
			webTestClient.get()
				.uri("/actuator/")
				.exchange()
				.expectStatus()
				.isEqualTo(expectedStatusWithTrailingSlash());
		});
	}

	protected HttpStatus expectedStatusWithTrailingSlash() {
		return HttpStatus.NOT_FOUND;
	}

	protected final WebApplicationContextRunner getContextRunner() {
		return createContextRunner().withPropertyValues("management.endpoints.web.exposure.include=*")
			.withUserConfiguration(BaseConfiguration.class, SecurityConfiguration.class)
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, SecurityAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					ManagementContextAutoConfiguration.class));

	}

	protected abstract WebApplicationContextRunner createContextRunner();

	protected WebTestClient getWebTestClient(AssertableWebApplicationContext context) {
		int port = context.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
			.getWebServer()
			.getPort();
		return WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.responseTimeout(Duration.ofMinutes(5))
			.build();
	}

	String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		TestEndpoint1 endpoint1() {
			return new TestEndpoint1();
		}

		@Bean
		TestEndpoint2 endpoint2() {
			return new TestEndpoint2();
		}

		@Bean
		TestEndpoint3 endpoint3() {
			return new TestEndpoint3();
		}

		@Bean
		TestServletEndpoint servletEndpoint() {
			return new TestServletEndpoint();
		}

	}

	@Endpoint(id = "e1")
	static class TestEndpoint1 {

		@ReadOperation
		Object getAll() {
			return "endpoint 1";
		}

	}

	@Endpoint(id = "e2")
	static class TestEndpoint2 {

		@ReadOperation
		Object getAll() {
			return "endpoint 2";
		}

	}

	@Endpoint(id = "e3")
	static class TestEndpoint3 {

		@ReadOperation
		Object getAll() {
			return null;
		}

	}

	@ServletEndpoint(id = "se1")
	static class TestServletEndpoint implements Supplier<EndpointServlet> {

		@Override
		public EndpointServlet get() {
			return new EndpointServlet(ExampleServlet.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		InMemoryUserDetailsManager userDetailsManager() {
			return new InMemoryUserDetailsManager(
					User.withUsername("user").password("{noop}password").roles("admin").build());
		}

		@Bean
		SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers(EndpointRequest.toLinks()).permitAll();
				requests.requestMatchers(EndpointRequest.to(TestEndpoint1.class)).permitAll();
				requests.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated();
				requests.anyRequest().hasRole("ADMIN");
			});
			http.httpBasic(withDefaults());
			return http.build();
		}

	}

	static class ExampleServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		}

	}

}
