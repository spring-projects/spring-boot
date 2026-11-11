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
 * Tests for {@link DockerCliComposePsResponse}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCliComposePsResponseTests {

	@Test
	void deserializeJson() throws IOException {
		String json = new ClassPathResource("docker-compose-ps.json", getClass())
			.getContentAsString(StandardCharsets.UTF_8);
		DockerCliComposePsResponse response = DockerJson.deserialize(json, DockerCliComposePsResponse.class);
		DockerCliComposePsResponse expected = new DockerCliComposePsResponse("f5af31dae7f6", "redis-docker-redis-1",
				"redis:7.0", "running");
		assertThat(response).isEqualTo(expected);
	}

}
