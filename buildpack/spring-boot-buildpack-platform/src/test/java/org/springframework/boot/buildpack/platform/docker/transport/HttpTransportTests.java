/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpTransport}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class HttpTransportTests {

	@Test
	void createWhenDockerHostVariableIsAddressReturnsRemote() {
		HttpTransport transport = HttpTransport.create(new DockerConnectionConfiguration.Host("tcp://192.168.1.0"));
		assertThat(transport).isInstanceOf(RemoteHttpClientTransport.class);
	}

	@Test
	void createWhenDockerHostVariableIsFileReturnsLocal(@TempDir Path tempDir) throws IOException {
		String dummySocketFilePath = Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath().toString();
		HttpTransport transport = HttpTransport.create(new DockerConnectionConfiguration.Host(dummySocketFilePath));
		assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
	}

	@Test
	void createWhenDockerHostVariableIsUnixSchemePrefixedFileReturnsLocal(@TempDir Path tempDir) throws IOException {
		String dummySocketFilePath = "unix://" + Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath();
		HttpTransport transport = HttpTransport.create(new DockerConnectionConfiguration.Host(dummySocketFilePath));
		assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
	}

}
