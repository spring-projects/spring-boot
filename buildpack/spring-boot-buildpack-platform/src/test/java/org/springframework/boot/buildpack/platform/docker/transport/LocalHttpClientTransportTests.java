/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;
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
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerConnectionConfiguration.Host(socketFilePath));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
	}

	@Test
	void createWhenDockerHostIsFileThatDoesNotExistReturnsTransport(@TempDir Path tempDir) {
		String socketFilePath = Paths.get(tempDir.toString(), "dummy").toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(new DockerConnectionConfiguration.Host(socketFilePath));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
	}

	@Test
	void createWhenDockerHostIsAddressReturnsTransport() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost
			.from(new DockerConnectionConfiguration.Host("tcp://192.168.1.2:2376"));
		LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
		assertThat(transport).isNotNull();
		assertThat(transport.getHost().toHostString()).isEqualTo("tcp://192.168.1.2:2376");
	}

}
