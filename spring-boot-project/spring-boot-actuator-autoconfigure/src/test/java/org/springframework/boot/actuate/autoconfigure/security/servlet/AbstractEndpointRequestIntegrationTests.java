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

import java.util.Base64;
import java.util.function.Supplier;

import org.jolokia.http.AgentServlet;
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
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;

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
		getContextRunner().withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO))
				.withPropertyValues("spring.security.user.password=password").run((context) -> {
					WebTestClient webTestClient = getWebTestClient(context);
					webTestClient.get().uri("/actuator/e2").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/actuator/e2").header("Authorization", getBasicAuth()).exchange()
							.expectStatus().isOk();
				});
	}

	@Test
	void toLinksShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator").exchange().expectStatus().isOk();
			webTestClient.get().uri("/actuator/").exchange().expectStatus().isOk();
		});
	}

	protected final WebApplicationContextRunner getContextRunner() {
		return createContextRunner().withPropertyValues("management.endpoints.web.exposure.include=*")
				.withUserConfiguration(BaseConfiguration.class, SecurityConfiguration.class).withConfiguration(
						AutoConfigurations.of(JacksonAutoConfiguration.class, SecurityAutoConfiguration.class,
								UserDetailsServiceAutoConfiguration.class, EndpointAutoConfiguration.class,
								WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class));

	}

	protected abstract WebApplicationContextRunner createContextRunner();

	protected WebTestClient getWebTestClient(AssertableWebApplicationContext context) {
		int port = context.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
				.getWebServer().getPort();
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
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
			return new EndpointServlet(AgentServlet.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
			return new WebSecurityConfigurerAdapter() {

				@Override
				protected void configure(HttpSecurity http) throws Exception {
					http.authorizeRequests((requests) -> {
						requests.requestMatchers(EndpointRequest.toLinks()).permitAll();
						requests.requestMatchers(EndpointRequest.to(TestEndpoint1.class)).permitAll();
						requests.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated();
						requests.anyRequest().hasRole("ADMIN");
					});
					http.httpBasic();
				}

			};
		}

	}

}
