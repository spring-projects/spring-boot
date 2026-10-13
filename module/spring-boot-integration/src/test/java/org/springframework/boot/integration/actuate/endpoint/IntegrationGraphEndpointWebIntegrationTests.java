/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.actuate.endpoint;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link IntegrationGraphEndpoint} exposed by Jersey, Spring MVC,
 * and WebFlux.
 *
 * @author Tim Ysewyn
 */
class IntegrationGraphEndpointWebIntegrationTests {

	@WebEndpointTest
	void graph(WebTestClient client) {
		client.get()
			.uri("/actuator/integrationgraph")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("contentDescriptor.providerVersion")
			.isNotEmpty()
			.jsonPath("contentDescriptor.providerFormatVersion")
			.isEqualTo(1.2f)
			.jsonPath("contentDescriptor.provider")
			.isEqualTo("spring-integration");
	}

	@WebEndpointTest
	void rebuild(WebTestClient client) {
		client.post()
			.uri("/actuator/integrationgraph")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isNoContent();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		IntegrationGraphEndpoint endpoint(IntegrationGraphServer integrationGraphServer) {
			return new IntegrationGraphEndpoint(integrationGraphServer);
		}

		@Bean
		IntegrationGraphServer integrationGraphServer() {
			return new IntegrationGraphServer();
		}

	}

}
