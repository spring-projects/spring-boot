/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.embedded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support when developing
 * a war application.
 *
 * @author Andy Wilkinson
 */
@EmbeddedServletContainerTest(packaging = "war",
		launchers = { BootRunApplicationLauncher.class, IdeApplicationLauncher.class })
class EmbeddedServletContainerWarDevelopmentIntegrationTests {

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

	@TestTemplate
	void webappResourcesAreAvailableViaHttp(RestClient rest) {
		ResponseEntity<String> entity = rest.get().uri("/webapp-resource.txt").retrieve().toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	void loaderClassesAreNotAvailableViaResourcePaths(RestClient rest) {
		ResponseEntity<String> entity = rest.get().uri("/resourcePaths").retrieve().toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(readLines(entity.getBody()))
			.noneMatch((resourcePath) -> resourcePath.startsWith("/org/springframework/boot/loader"));
	}

	private List<String> readLines(String input) {
		if (input == null) {
			return Collections.emptyList();
		}
		try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
			return reader.lines().toList();
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read lines from input '" + input + "'");
		}
	}

}
