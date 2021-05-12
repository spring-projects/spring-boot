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

package org.springframework.boot.context.embedded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support using war
 * packaging.
 *
 * @author Andy Wilkinson
 */
@EmbeddedServletContainerTest(packaging = "war",
		launchers = { PackagedApplicationLauncher.class, ExplodedApplicationLauncher.class })
class EmbeddedServletContainerWarPackagingIntegrationTests {

	@TestTemplate
	void nestedMetaInfResourceIsAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/nested-meta-inf-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	@DisabledOnOs(OS.WINDOWS)
	void nestedMetaInfResourceWithNameThatContainsReservedCharactersIsAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity(
				"/nested-reserved-%21%23%24%25%26%28%29%2A%2B%2C%3A%3D%3F%40%5B%5D-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("encoded-name");
	}

	@TestTemplate
	void nestedMetaInfResourceIsAvailableViaServletContext(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/servletContext?/nested-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	void nestedJarIsNotAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/WEB-INF/lib/resources-1.0.jar", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@TestTemplate
	void applicationClassesAreNotAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest
				.getForEntity("/WEB-INF/classes/com/example/ResourceHandlingApplication.class", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@TestTemplate
	void webappResourcesAreAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/webapp-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	void loaderClassesAreNotAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/org/springframework/boot/loader/Launcher.class",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		entity = rest.getForEntity("/org/springframework/../springframework/boot/loader/Launcher.class", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@TestTemplate
	void loaderClassesAreNotAvailableViaResourcePaths(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/resourcePaths", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(readLines(entity.getBody()))
				.noneMatch((resourcePath) -> resourcePath.startsWith("/org/springframework/boot/loader"));
	}

	@TestTemplate
	void conditionalOnWarDeploymentBeanIsNotAvailableForEmbeddedServer(RestTemplate rest) {
		assertThat(rest.getForEntity("/always", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(rest.getForEntity("/conditionalOnWar", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	private List<String> readLines(String input) {
		if (input == null) {
			return Collections.emptyList();
		}
		try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
			return reader.lines().collect(Collectors.toList());
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read lines from input '" + input + "'");
		}
	}

}
