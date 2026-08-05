/*
 * Copyright 2012-present the original author or authors.
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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCli.DockerCommands;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerCli}.
 *
 * @author Martin Imobersteg
 */
class DockerCliTests {

	private static final String[] DOCKER_VERSION = { "docker", "version", "--format", "{{.Client.Version}}" };

	private static final String[] PODMAN_VERSION = { "podman", "version", "--format", "{{.Client.Version}}" };

	private static final String[] DOCKER_COMPOSE_VERSION = { "docker", "compose", "version", "--format", "json" };

	private static final String[] DOCKER_COMPOSE_BINARY_VERSION = { "docker-compose", "version", "--format", "json" };

	private static final String[] PODMAN_COMPOSE_VERSION = { "podman", "compose", "version", "--format", "json" };

	private final ProcessRunner processRunner = mock(ProcessRunner.class);

	@Test
	void usesDockerWhenDockerIsInstalled() {
		given(this.processRunner.run(DOCKER_VERSION)).willReturn("27.3.1");
		given(this.processRunner.run(DOCKER_COMPOSE_VERSION)).willReturn("{\"version\":\"v2.30.3\"}");
		DockerCommands commands = new DockerCommands(this.processRunner);
		assertThat(commands.get(Type.DOCKER).command()).containsExactly("docker");
		assertThat(commands.get(Type.DOCKER_COMPOSE).command()).containsExactly("docker", "compose");
		assertThat(commands.get(Type.DOCKER_COMPOSE).version()).isEqualTo("v2.30.3");
	}

	@Test
	void fallsBackToDockerComposeBinaryWhenDockerComposePluginIsNotInstalled() {
		given(this.processRunner.run(DOCKER_VERSION)).willReturn("27.3.1");
		willThrow(processExit(DOCKER_COMPOSE_VERSION)).given(this.processRunner).run(DOCKER_COMPOSE_VERSION);
		given(this.processRunner.run(DOCKER_COMPOSE_BINARY_VERSION)).willReturn("{\"version\":\"v2.30.3\"}");
		DockerCommands commands = new DockerCommands(this.processRunner);
		assertThat(commands.get(Type.DOCKER_COMPOSE).command()).containsExactly("docker-compose");
	}

	@Test
	void fallsBackToDockerComposeBinaryWhenDockerComposePluginDoesNotReturnJson() {
		// gh-43440: 'docker' aliased to 'podman' can return non-JSON output here
		given(this.processRunner.run(DOCKER_VERSION)).willReturn("27.3.1");
		given(this.processRunner.run(DOCKER_COMPOSE_VERSION)).willReturn("");
		given(this.processRunner.run(DOCKER_COMPOSE_BINARY_VERSION)).willReturn("{\"version\":\"v2.30.3\"}");
		DockerCommands commands = new DockerCommands(this.processRunner);
		assertThat(commands.get(Type.DOCKER_COMPOSE).command()).containsExactly("docker-compose");
	}

	@Test
	void usesPodmanWhenDockerIsNotInstalled() {
		willThrow(processStart(DOCKER_VERSION)).given(this.processRunner).run(DOCKER_VERSION);
		given(this.processRunner.run(PODMAN_VERSION)).willReturn("5.8.1");
		given(this.processRunner.run(PODMAN_COMPOSE_VERSION)).willReturn("{\"version\":\"v2.30.3\"}");
		DockerCommands commands = new DockerCommands(this.processRunner);
		assertThat(commands.get(Type.DOCKER).command()).containsExactly("podman");
		assertThat(commands.get(Type.DOCKER).version()).isEqualTo("5.8.1");
		assertThat(commands.get(Type.DOCKER_COMPOSE).command()).containsExactly("podman", "compose");
		assertThat(commands.get(Type.DOCKER_COMPOSE).version()).isEqualTo("v2.30.3");
	}

	@Test
	void doesNotUsePodmanComposeBinaryWhenPodmanComposeIsNotUsable() {
		willThrow(processStart(DOCKER_VERSION)).given(this.processRunner).run(DOCKER_VERSION);
		given(this.processRunner.run(PODMAN_VERSION)).willReturn("5.8.1");
		willThrow(processExit(PODMAN_COMPOSE_VERSION)).given(this.processRunner).run(PODMAN_COMPOSE_VERSION);
		assertThatExceptionOfType(DockerProcessStartException.class)
			.isThrownBy(() -> new DockerCommands(this.processRunner))
			.withMessageContaining("Unable to use 'podman compose'")
			.withMessageContaining("Is podman correctly installed?");
	}

	@Test
	void failsWhenNeitherDockerNorPodmanIsInstalled() {
		willThrow(processStart(DOCKER_VERSION)).given(this.processRunner).run(DOCKER_VERSION);
		willThrow(processStart(PODMAN_VERSION)).given(this.processRunner).run(PODMAN_VERSION);
		assertThatExceptionOfType(DockerProcessStartException.class)
			.isThrownBy(() -> new DockerCommands(this.processRunner))
			.withMessageContaining("Unable to start docker or podman process")
			.satisfies((ex) -> assertThat(ex.getSuppressed()).hasSize(1));
	}

	@Test
	void failsWithDockerNotRunningWhenDockerDaemonIsNotRunning() {
		willThrow(processExit(DOCKER_VERSION, "Cannot connect to the Docker daemon at unix:///var/run/docker.sock."))
			.given(this.processRunner)
			.run(DOCKER_VERSION);
		assertThatExceptionOfType(DockerNotRunningException.class)
			.isThrownBy(() -> new DockerCommands(this.processRunner))
			.satisfies((ex) -> assertThat(ex.getErrorOutput()).contains("Cannot connect to the Docker daemon"));
	}

	@Test
	void failsWithDockerNotRunningWhenPodmanMachineIsNotRunning() {
		willThrow(processStart(DOCKER_VERSION)).given(this.processRunner).run(DOCKER_VERSION);
		willThrow(processExit(PODMAN_VERSION,
				"Cannot connect to Podman. Please verify your connection to the Linux "
						+ "system using `podman system connection list`"))
			.given(this.processRunner)
			.run(PODMAN_VERSION);
		assertThatExceptionOfType(DockerNotRunningException.class)
			.isThrownBy(() -> new DockerCommands(this.processRunner))
			.satisfies((ex) -> assertThat(ex.getErrorOutput()).contains("Cannot connect to Podman"));
	}

	private ProcessStartException processStart(String[] command) {
		return new ProcessStartException(command, new IOException("Cannot run program \"%s\"".formatted(command[0])));
	}

	private ProcessExitException processExit(String[] command) {
		return processExit(command, "");
	}

	private ProcessExitException processExit(String[] command, String stdErr) {
		return new ProcessExitException(1, command, "", stdErr);
	}

}
