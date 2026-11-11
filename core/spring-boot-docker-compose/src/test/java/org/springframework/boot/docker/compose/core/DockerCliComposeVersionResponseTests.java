/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCliComposeVersionResponse}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCliComposeVersionResponseTests {

	@Test
	void deserializeJson() throws IOException {
		String json = new ClassPathResource("docker-compose-version.json", getClass())
			.getContentAsString(StandardCharsets.UTF_8);
		DockerCliComposeVersionResponse response = DockerJson.deserialize(json, DockerCliComposeVersionResponse.class);
		DockerCliComposeVersionResponse expected = new DockerCliComposeVersionResponse("123");
		assertThat(response).isEqualTo(expected);
	}

}
