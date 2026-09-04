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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerCli}.
 *
 * @author Somil Jain
 */
class DockerCliTests {

	private final ProcessRunner processRunner = mock(ProcessRunner.class);

	@BeforeEach
	void setUp() {
		lenient().when(this.processRunner.run(any(), any(String[].class))).thenReturn("[]");
	}

	@Test
	void discoversDockerWithComposePlugin() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}")).willReturn("24.0.0");
		given(this.processRunner.run("docker", "compose", "version", "--format", "json"))
			.willReturn("{\"version\": \"2.17.0\"}");
		DockerCli cli = new DockerCli(this.processRunner);

		cli.run(new DockerCliCommand.Context());

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
		then(this.processRunner).should().run(any(), captor.capture());
		assertThat(captor.getValue()).containsExactly("docker", "context", "ls", "--format={{ json . }}");
	}

	@Test
	void discoversDockerWithStandaloneComposeFallback() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}")).willReturn("24.0.0");
		given(this.processRunner.run("docker", "compose", "version", "--format", "json"))
			.willThrow(new ProcessExitException(1, new String[] { "docker", "compose" }, "", "No such command"));
		given(this.processRunner.run("docker-compose", "version", "--format", "json"))
			.willReturn("{\"version\": \"1.29.2\"}");
		lenient().when(this.processRunner.run(any(), any(String[].class))).thenReturn("{}");
		DockerCli cli = new DockerCli(this.processRunner);

		cli.run(new DockerCliCommand.ComposeConfig());

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
		then(this.processRunner).should().run(any(), captor.capture());
		assertThat(captor.getValue()).containsExactly("docker-compose", "--ansi", "never", "config", "--format=json");
	}

	@Test
	void discoversPodmanWithComposePluginWhenDockerMissing() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}"))
			.willThrow(new ProcessStartException(new String[] { "docker" }, new IOException("missing")));
		given(this.processRunner.run("podman", "version", "--format", "{{.Client.Version}}")).willReturn("4.5.0");
		given(this.processRunner.run("podman", "compose", "version")).willReturn("podman-compose version 1.0.6");
		DockerCli cli = new DockerCli(this.processRunner);

		List<?> result = cli.run(new DockerCliCommand.Context());
		assertThat(result).isEmpty();
	}

	@Test
	void discoversPodmanWithStandaloneComposeFallback() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}"))
			.willThrow(new ProcessStartException(new String[] { "docker" }, new IOException("missing")));
		given(this.processRunner.run("podman", "version", "--format", "{{.Client.Version}}")).willReturn("4.5.0");
		given(this.processRunner.run("podman", "compose", "version"))
			.willThrow(new ProcessExitException(1, new String[] { "podman", "compose" }, "", "No such command"));
		given(this.processRunner.run("podman-compose", "version")).willReturn("podman-compose version 1.0.6");
		lenient().when(this.processRunner.run(any(), any(String[].class))).thenReturn("{}");
		DockerCli cli = new DockerCli(this.processRunner);

		cli.run(new DockerCliCommand.ComposeConfig());

		ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
		then(this.processRunner).should().run(any(), captor.capture());
		assertThat(captor.getValue()).containsExactly("podman-compose", "config");
	}

	@Test
	void preservesDockerNotRunningException() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}")).willThrow(
				new ProcessExitException(1, new String[] { "docker" }, "", "Cannot connect to the Docker daemon"));

		assertThatExceptionOfType(DockerNotRunningException.class).isThrownBy(() -> new DockerCli(this.processRunner));
	}

	@Test
	void throwsDockerProcessStartExceptionWhenNeitherEngineAvailable() {
		given(this.processRunner.run("docker", "version", "--format", "{{.Client.Version}}"))
			.willThrow(new ProcessStartException(new String[] { "docker" }, new IOException("missing docker")));
		given(this.processRunner.run("podman", "version", "--format", "{{.Client.Version}}"))
			.willThrow(new ProcessStartException(new String[] { "podman" }, new IOException("missing podman")));

		assertThatExceptionOfType(DockerProcessStartException.class).isThrownBy(() -> new DockerCli(this.processRunner))
			.withMessageContaining("Unable to find 'docker' or 'podman' executable");
	}

}
