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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link EndpointRequest} with Spring MVC.
 *
 * @author Madhura Bhave
 */
class MvcEndpointRequestIntegrationTests extends AbstractEndpointRequestIntegrationTests {

	@Test
	void toLinksWhenServletPathSetShouldMatch() {
		getContextRunner().withPropertyValues("spring.mvc.servlet.path=/admin").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/admin/actuator/").exchange().expectStatus().isNotFound();
			webTestClient.get().uri("/admin/actuator").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toEndpointWhenServletPathSetShouldMatch() {
		getContextRunner().withPropertyValues("spring.mvc.servlet.path=/admin").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/admin/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toAnyEndpointWhenServletPathSetShouldMatch() {
		getContextRunner()
			.withPropertyValues("spring.mvc.servlet.path=/admin", "spring.security.user.password=password")
			.run((context) -> {
				WebTestClient webTestClient = getWebTestClient(context);
				webTestClient.get().uri("/admin/actuator/e2").exchange().expectStatus().isUnauthorized();
				webTestClient.get()
					.uri("/admin/actuator/e2")
					.header("Authorization", getBasicAuth())
					.exchange()
					.expectStatus()
					.isOk();
			});
	}

	@Test
	void toAnyEndpointShouldMatchServletEndpoint() {
		getContextRunner()
			.withPropertyValues("spring.security.user.password=password",
					"management.endpoints.web.exposure.include=se1")
			.run((context) -> {
				WebTestClient webTestClient = getWebTestClient(context);
				webTestClient.get().uri("/actuator/se1").exchange().expectStatus().isUnauthorized();
				webTestClient.get()
					.uri("/actuator/se1")
					.header("Authorization", getBasicAuth())
					.exchange()
					.expectStatus()
					.isOk();
				webTestClient.get().uri("/actuator/se1/list").exchange().expectStatus().isUnauthorized();
				webTestClient.get()
					.uri("/actuator/se1/list")
					.header("Authorization", getBasicAuth())
					.exchange()
					.expectStatus()
					.isOk();
			});
	}

	@Test
	void toAnyEndpointWhenServletPathSetShouldMatchServletEndpoint() {
		getContextRunner()
			.withPropertyValues("spring.mvc.servlet.path=/admin", "spring.security.user.password=password",
					"management.endpoints.web.exposure.include=se1")
			.run((context) -> {
				WebTestClient webTestClient = getWebTestClient(context);
				webTestClient.get().uri("/admin/actuator/se1").exchange().expectStatus().isUnauthorized();
				webTestClient.get()
					.uri("/admin/actuator/se1")
					.header("Authorization", getBasicAuth())
					.exchange()
					.expectStatus()
					.isOk();
				webTestClient.get().uri("/admin/actuator/se1/list").exchange().expectStatus().isUnauthorized();
				webTestClient.get()
					.uri("/admin/actuator/se1/list")
					.header("Authorization", getBasicAuth())
					.exchange()
					.expectStatus()
					.isOk();
			});
	}

	@Override
	protected WebApplicationContextRunner createContextRunner() {
		return new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withUserConfiguration(WebMvcEndpointConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class WebMvcEndpointConfiguration {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
