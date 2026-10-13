/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.actuate.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
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
			.withUserConfiguration(InfrastructureTestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class InfrastructureTestConfiguration {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
