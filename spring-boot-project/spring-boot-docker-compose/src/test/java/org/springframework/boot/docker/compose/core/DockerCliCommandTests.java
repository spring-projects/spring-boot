/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeVersion;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCliCommand}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCliCommandTests {

	private static final ComposeVersion COMPOSE_VERSION = ComposeVersion.of("2.31.0");

	@Test
	void context() {
		DockerCliCommand<?> command = new DockerCliCommand.Context();
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("context", "ls", "--format={{ json . }}");
		assertThat(command.deserialize("[]")).isInstanceOf(List.class);
	}

	@Test
	void inspect() {
		DockerCliCommand<?> command = new DockerCliCommand.Inspect(List.of("123", "345"));
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("inspect", "--format={{ json . }}", "123",
				"345");
		assertThat(command.deserialize("[]")).isInstanceOf(List.class);
	}

	@Test
	void composeConfig() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposeConfig();
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("config", "--format=json");
		assertThat(command.deserialize("{}")).isInstanceOf(DockerCliComposeConfigResponse.class);
	}

	@Test
	void composePs() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposePs();
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("ps", "--orphans=false", "--format=json");
		assertThat(command.deserialize("[]")).isInstanceOf(List.class);
	}

	@Test
	void composePsWhenLessThanV224() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposePs();
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getCommand(ComposeVersion.of("2.23"))).containsExactly("ps", "--format=json");
		assertThat(command.deserialize("[]")).isInstanceOf(List.class);
	}

	@Test
	void composeUp() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposeUp(LogLevel.INFO, List.of("--renew-anon-volumes"));
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getLogLevel()).isEqualTo(LogLevel.INFO);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("up", "--no-color", "--detach", "--wait",
				"--renew-anon-volumes");
		assertThat(command.deserialize("[]")).isNull();
	}

	@Test
	void composeDown() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposeDown(Duration.ofSeconds(1),
				List.of("--remove-orphans"));
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("down", "--timeout", "1", "--remove-orphans");
		assertThat(command.deserialize("[]")).isNull();
	}

	@Test
	void composeStart() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposeStart(LogLevel.INFO, List.of("--dry-run"));
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getLogLevel()).isEqualTo(LogLevel.INFO);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("start", "--dry-run");
		assertThat(command.deserialize("[]")).isNull();
	}

	@Test
	void composeStop() {
		DockerCliCommand<?> command = new DockerCliCommand.ComposeStop(Duration.ofSeconds(1), List.of("--dry-run"));
		assertThat(command.getType()).isEqualTo(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command.getCommand(COMPOSE_VERSION)).containsExactly("stop", "--timeout", "1", "--dry-run");
		assertThat(command.deserialize("[]")).isNull();
	}

	@Test
	void composeVersionTests() {
		ComposeVersion version = ComposeVersion.of("2.31.0-desktop");
		assertThat(version.major()).isEqualTo(2);
		assertThat(version.minor()).isEqualTo(31);
		assertThat(version.isLessThan(1, 0)).isFalse();
		assertThat(version.isLessThan(2, 0)).isFalse();
		assertThat(version.isLessThan(2, 31)).isFalse();
		assertThat(version.isLessThan(2, 32)).isTrue();
		assertThat(version.isLessThan(3, 0)).isTrue();
		ComposeVersion versionWithPrefix = ComposeVersion.of("v2.31.0-desktop");
		assertThat(versionWithPrefix.major()).isEqualTo(2);
		assertThat(versionWithPrefix.minor()).isEqualTo(31);
	}

}
