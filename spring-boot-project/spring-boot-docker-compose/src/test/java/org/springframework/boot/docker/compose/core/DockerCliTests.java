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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCli.DockerComposeOptions;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DockerCliTests {
	@Test
	void createCommandWithDockerComposeShouldIncludeCorrectOptions() {
		ProcessRunner mockProcessRunner = mock(ProcessRunner.class);
		when(mockProcessRunner.run(any(String[].class))).thenReturn("{\"version\":\"1.0.0\"}");

		DockerComposeFile mockFile = mock(DockerComposeFile.class);
		when(mockFile.getFiles()).thenReturn(List.of(new File("docker-compose.yml")));

		DockerCli.DockerComposeOptions options = new DockerCli.DockerComposeOptions(mockFile, Set.of("dev"), List.of("--verbose"));
		DockerCli cli = new DockerCli(mockProcessRunner, options);

		List<String> command = cli.createCommand(DockerCliCommand.Type.DOCKER_COMPOSE);
		assertThat(command).contains("--file", "docker-compose.yml", "--profile", "dev", "--verbose");
	}

	@Test
	void fileConstructorShouldInitializeCorrectly_whenDockerIsRunning() {
		// assume docker is running
		Assumptions.assumeTrue(new File("/var/run/docker.sock").exists() || System.getProperty("os.name").startsWith("Windows"));

		DockerComposeFile mockFile = mock(DockerComposeFile.class);
		when(mockFile.getFiles()).thenReturn(List.of(new File("docker-compose.yml")));

		DockerCli.DockerComposeOptions options = new DockerCli.DockerComposeOptions(mockFile, Set.of("dev"), List.of("--verbose"));

		DockerCli cli = new DockerCli(new File("."), options);

		List<String> command = cli.createCommand(DockerCliCommand.Type.DOCKER_COMPOSE);

		assertThat(command).contains("--file", "docker-compose.yml", "--profile", "dev", "--verbose");
	}

}
