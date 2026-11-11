/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.docs.integration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.integration.actuate.endpoint.IntegrationGraphEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for generating documentation describing the {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 */
class IntegrationGraphEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void graph() {
		assertThat(this.mvc.get().uri("/actuator/integrationgraph")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("integrationgraph/graph"));
	}

	@Test
	void rebuild() {
		assertThat(this.mvc.post().uri("/actuator/integrationgraph")).hasStatus(HttpStatus.NO_CONTENT)
			.apply(MockMvcRestDocumentation.document("integrationgraph/rebuild"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		IntegrationGraphServer integrationGraphServer() {
			return new IntegrationGraphServer();
		}

		@Bean
		IntegrationGraphEndpoint endpoint(IntegrationGraphServer integrationGraphServer) {
			return new IntegrationGraphEndpoint(integrationGraphServer);
		}

	}

}
