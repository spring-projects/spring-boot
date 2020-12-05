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

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link EndpointRequest} with Jersey.
 *
 * @author Madhura Bhave
 */
class JerseyEndpointRequestIntegrationTests extends AbstractEndpointRequestIntegrationTests {

	@Test
	void toLinksWhenApplicationPathSetShouldMatch() {
		getContextRunner().withPropertyValues("spring.jersey.application-path=/admin").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/admin/actuator/").exchange().expectStatus().isOk();
			webTestClient.get().uri("/admin/actuator").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toEndpointWhenApplicationPathSetShouldMatch() {
		getContextRunner().withPropertyValues("spring.jersey.application-path=/admin").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/admin/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toAnyEndpointWhenApplicationPathSetShouldMatch() {
		getContextRunner()
				.withPropertyValues("spring.jersey.application-path=/admin", "spring.security.user.password=password")
				.run((context) -> {
					WebTestClient webTestClient = getWebTestClient(context);
					webTestClient.get().uri("/admin/actuator/e2").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/admin/actuator/e2").header("Authorization", getBasicAuth()).exchange()
							.expectStatus().isOk();
				});
	}

	@Test
	void toAnyEndpointShouldMatchServletEndpoint() {
		getContextRunner().withPropertyValues("spring.security.user.password=password",
				"management.endpoints.web.exposure.include=se1").run((context) -> {
					WebTestClient webTestClient = getWebTestClient(context);
					webTestClient.get().uri("/actuator/se1").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/actuator/se1").header("Authorization", getBasicAuth()).exchange()
							.expectStatus().isOk();
					webTestClient.get().uri("/actuator/se1/list").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/actuator/se1/list").header("Authorization", getBasicAuth()).exchange()
							.expectStatus().isOk();
				});
	}

	@Test
	void toAnyEndpointWhenApplicationPathSetShouldMatchServletEndpoint() {
		getContextRunner().withPropertyValues("spring.jersey.application-path=/admin",
				"spring.security.user.password=password", "management.endpoints.web.exposure.include=se1")
				.run((context) -> {
					WebTestClient webTestClient = getWebTestClient(context);
					webTestClient.get().uri("/admin/actuator/se1").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/admin/actuator/se1").header("Authorization", getBasicAuth()).exchange()
							.expectStatus().isOk();
					webTestClient.get().uri("/admin/actuator/se1/list").exchange().expectStatus().isUnauthorized();
					webTestClient.get().uri("/admin/actuator/se1/list").header("Authorization", getBasicAuth())
							.exchange().expectStatus().isOk();
				});
	}

	@Override
	protected WebApplicationContextRunner createContextRunner() {
		return new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
				.withUserConfiguration(JerseyEndpointConfiguration.class)
				.withConfiguration(AutoConfigurations.of(JerseyAutoConfiguration.class));
	}

	@Configuration
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class JerseyEndpointConfiguration {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

	}

}
