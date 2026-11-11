/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.reactive.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Integration tests for the Actuator's WebFlux {@link ControllerEndpoint controller
 * endpoints}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("removal")
class ControllerEndpointWebFluxIntegrationTests {

	private @Nullable AnnotationConfigReactiveWebApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void endpointsCanBeAccessed() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		this.context.register(DefaultConfiguration.class, ExampleController.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		this.context.refresh();
		WebTestClient webClient = WebTestClient.bindToApplicationContext(this.context).build();
		webClient.get().uri("/actuator/example").exchange().expectStatus().isOk();
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebFluxAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@RestControllerEndpoint(id = "example")
	static class ExampleController {

		@GetMapping("/")
		String example() {
			return "Example";
		}

	}

}
