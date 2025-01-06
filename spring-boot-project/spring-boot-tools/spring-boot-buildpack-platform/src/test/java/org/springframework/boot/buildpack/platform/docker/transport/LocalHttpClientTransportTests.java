/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalHttpClientTransport}
 *
 * @author Scott Frederick
 */
class LocalHttpClientTransportTests {

	@Test
	void createWhenDockerHostIsFileReturnsTransport(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration.forAddress(socketFilePath));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
	}

	@Test
	void createWhenDockerHostIsFileThatDoesNotExistReturnsTransport(@TempDir Path tempDir) {
		String socketFilePath = Paths.get(tempDir.toString(), "dummy").toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration.forAddress(socketFilePath));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
	}

	@Test
	void createWhenDockerHostIsAddressReturnsTransport() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376"));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo("tcp://192.168.1.2:2376");
	}

}
