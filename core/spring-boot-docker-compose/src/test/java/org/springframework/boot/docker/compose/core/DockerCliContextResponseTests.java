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
 * Tests for {@link DockerCliContextResponse}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCliContextResponseTests {

	@Test
	void deserializeJson() throws IOException {
		String json = new ClassPathResource("docker-context.json", getClass())
			.getContentAsString(StandardCharsets.UTF_8);
		DockerCliContextResponse response = DockerJson.deserialize(json, DockerCliContextResponse.class);
		DockerCliContextResponse expected = new DockerCliContextResponse("default", true,
				"unix:///var/run/docker.sock");
		assertThat(response).isEqualTo(expected);
	}

	@Test
	void deserializePodmanJson() throws IOException {
		String json = new ClassPathResource("docker-context-podman.json", getClass())
			.getContentAsString(StandardCharsets.UTF_8);
		DockerCliContextResponse response = DockerJson.deserialize(json, DockerCliContextResponse.class);
		DockerCliContextResponse expected = new DockerCliContextResponse("podman-machine-default", false, null);
		assertThat(response).isEqualTo(expected);
	}

}
