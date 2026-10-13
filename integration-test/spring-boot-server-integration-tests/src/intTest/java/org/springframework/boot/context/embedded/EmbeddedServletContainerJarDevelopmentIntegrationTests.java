/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.embedded;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support when developing
 * a jar application.
 *
 * @author Andy Wilkinson
 */
@EmbeddedServletContainerTest(packaging = "jar",
		launchers = { BootRunApplicationLauncher.class, IdeApplicationLauncher.class })
class EmbeddedServletContainerJarDevelopmentIntegrationTests {

	@TestTemplate
	void metaInfResourceFromDependencyIsAvailableViaHttp(RestClient rest) {
		ResponseEntity<String> entity = rest.get()
			.uri("/nested-meta-inf-resource.txt")
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	@DisabledOnOs(OS.WINDOWS)
	void metaInfResourceFromDependencyWithNameThatContainsReservedCharactersIsAvailableViaHttp(RestClient rest) {
		ResponseEntity<String> entity = rest.get()
			.uri("/nested-reserved-%21%23%24%25%26%28%29%2A%2B%2C%3A%3D%3F%40%5B%5D-meta-inf-resource.txt")
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("encoded-name");
	}

	@TestTemplate
	void metaInfResourceFromDependencyIsAvailableViaServletContext(RestClient rest) {
		ResponseEntity<String> entity = rest.get()
			.uri("/servletContext?/nested-meta-inf-resource.txt")
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
