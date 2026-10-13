/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.sbom;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link SbomEndpoint} exposed by Jersey, Spring MVC, and WebFlux
 * in SPDX format.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointSpdxWebIntegrationTests {

	@WebEndpointTest
	void shouldReturnSbomContent(WebTestClient client) {
		client.get()
			.uri("/actuator/sbom/application")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType("application/spdx+json"))
			.expectBody()
			.jsonPath("$.spdxVersion")
			.isEqualTo("SPDX-2.3");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		SbomProperties sbomProperties() {
			SbomProperties properties = new SbomProperties();
			properties.getApplication().setLocation("classpath:org/springframework/boot/actuate/sbom/spdx.json");
			return properties;
		}

		@Bean
		SbomEndpoint sbomEndpoint(SbomProperties properties, ResourceLoader resourceLoader) {
			return new SbomEndpoint(properties, resourceLoader);
		}

		@Bean
		SbomEndpointWebExtension sbomEndpointWebExtension(SbomEndpoint sbomEndpoint, SbomProperties properties) {
			return new SbomEndpointWebExtension(sbomEndpoint, properties);
		}

	}

}
