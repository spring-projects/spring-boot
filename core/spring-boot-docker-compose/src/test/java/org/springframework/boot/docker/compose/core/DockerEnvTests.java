/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DockerEnv}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerEnvTests {

	@Test
	void createWhenEnvIsNullReturnsEmpty() {
		DockerEnv env = new DockerEnv(null);
		assertThat(env.asMap()).isEmpty();
	}

	@Test
	void createWhenEnvIsEmptyReturnsEmpty() {
		DockerEnv env = new DockerEnv(Collections.emptyList());
		assertThat(env.asMap()).isEmpty();
	}

	@Test
	void createParsesEnv() {
		DockerEnv env = new DockerEnv(List.of("a=b", "c"));
		assertThat(env.asMap()).containsExactly(entry("a", "b"), entry("c", null));
	}

}
