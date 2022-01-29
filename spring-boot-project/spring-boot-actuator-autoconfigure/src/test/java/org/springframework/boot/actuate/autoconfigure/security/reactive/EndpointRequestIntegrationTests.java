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

package org.springframework.boot.actuate.autoconfigure.security.reactive;

import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link EndpointRequest}.
 *
 * @author Chris Bono
 */
class EndpointRequestIntegrationTests {

	@Test
	void toEndpointShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toEndpointPostShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.post().uri("/actuator/e1").exchange().expectStatus().isUnauthorized();
			webTestClient.post().uri("/actuator/e1").header("Authorization", getBasicAuth()).exchange().expectStatus()
					.isNoContent();
		});
	}

	@Test
	void toAllEndpointsShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e2").exchange().expectStatus().isUnauthorized();
			webTestClient.get().uri("/actuator/e2").header("Authorization", getBasicAuth()).exchange().expectStatus()
					.isOk();
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

	protected final ReactiveWebApplicationContextRunner getContextRunner() {
		return createContextRunner().withPropertyValues("management.endpoints.web.exposure.include=*")
				.withUserConfiguration(BaseConfiguration.class, SecurityConfiguration.class).withConfiguration(
						AutoConfigurations.of(JacksonAutoConfiguration.class, ReactiveSecurityAutoConfiguration.class,
								ReactiveUserDetailsServiceAutoConfiguration.class, EndpointAutoConfiguration.class,
								WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class));

	}

	protected ReactiveWebApplicationContextRunner createContextRunner() {
		return new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withUserConfiguration(WebEndpointConfiguration.class)
				.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class,
						HttpMessageConvertersAutoConfiguration.class, WebFluxAutoConfiguration.class));
	}

	protected WebTestClient getWebTestClient(AssertableReactiveWebApplicationContext context) {
		int port = context.getSourceApplicationContext(AnnotationConfigReactiveWebServerApplicationContext.class)
				.getWebServer().getPort();
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).responseTimeout(Duration.ofMinutes(5))
				.build();
	}

	private String getBasicAuth() {
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

	}

	@Endpoint(id = "e1")
	static class TestEndpoint1 {

		@ReadOperation
		Object getAll() {
			return "endpoint 1";
		}

		@WriteOperation
		void setAll() {
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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class WebEndpointConfiguration {

		@Bean
		TomcatReactiveWebServerFactory tomcat() {
			return new TomcatReactiveWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			// Required to allow POST calls
			http.csrf().disable();

			http.authorizeExchange((exchanges) -> {
				exchanges.matchers(EndpointRequest.toLinks()).permitAll();
				exchanges.matchers(EndpointRequest.to(TestEndpoint1.class).withHttpMethod(HttpMethod.POST))
						.authenticated();
				exchanges.matchers(EndpointRequest.to(TestEndpoint1.class)).permitAll();
				exchanges.matchers(EndpointRequest.toAnyEndpoint()).authenticated();
				exchanges.anyExchange().hasRole("ADMIN");
			});
			http.httpBasic();
			return http.build();
		}

	}

}
