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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResolvedDockerHost}.
 *
 * @author Scott Frederick
 */
class ResolvedDockerHostTests {

	private final Map<String, String> environment = new LinkedHashMap<>();

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void resolveWhenDockerHostIsNullReturnsLinuxDefault() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("/var/run/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void resolveWhenDockerHostIsNullReturnsWindowsDefault() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("//./pipe/docker_engine");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void resolveWhenDockerHostAddressIsNullReturnsLinuxDefault() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress(null));
		assertThat(dockerHost.getAddress()).isEqualTo("/var/run/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsLocalReturnsAddress(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress(socketFilePath));
		assertThat(dockerHost.isLocalFileReference()).isTrue();
		assertThat(dockerHost.isRemote()).isFalse();
		assertThat(dockerHost.getAddress()).isEqualTo(socketFilePath);
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsLocalWithSchemeReturnsAddress(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("unix://" + socketFilePath));
		assertThat(dockerHost.isLocalFileReference()).isTrue();
		assertThat(dockerHost.isRemote()).isFalse();
		assertThat(dockerHost.getAddress()).isEqualTo(socketFilePath);
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsHttpReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("http://docker.example.com"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("http://docker.example.com");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsHttpsReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("https://docker.example.com", true, "/cert-path"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("https://docker.example.com");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isEqualTo("/cert-path");
	}

	@Test
	void resolveWhenDockerHostAddressIsTcpReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("tcp://192.168.99.100:2376", true, "/cert-path"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("tcp://192.168.99.100:2376");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isEqualTo("/cert-path");
	}

	@Test
	void resolveWhenEnvironmentAddressIsLocalReturnsAddress(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		this.environment.put("DOCKER_HOST", socketFilePath);
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("/unused"));
		assertThat(dockerHost.isLocalFileReference()).isTrue();
		assertThat(dockerHost.isRemote()).isFalse();
		assertThat(dockerHost.getAddress()).isEqualTo(socketFilePath);
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenEnvironmentAddressIsLocalWithSchemeReturnsAddress(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		this.environment.put("DOCKER_HOST", "unix://" + socketFilePath);
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("/unused"));
		assertThat(dockerHost.isLocalFileReference()).isTrue();
		assertThat(dockerHost.isRemote()).isFalse();
		assertThat(dockerHost.getAddress()).isEqualTo(socketFilePath);
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenEnvironmentAddressIsTcpReturnsAddress() {
		this.environment.put("DOCKER_HOST", "tcp://192.168.99.100:2376");
		this.environment.put("DOCKER_TLS_VERIFY", "1");
		this.environment.put("DOCKER_CERT_PATH", "/cert-path");
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forAddress("tcp://1.1.1.1"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("tcp://192.168.99.100:2376");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isEqualTo("/cert-path");
	}

	@Test
	void resolveWithDockerHostContextReturnsAddress() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				DockerHostConfiguration.forContext("test-context"));
		assertThat(dockerHost.getAddress()).isEqualTo("/home/user/.docker/docker.sock");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isNotNull();
	}

	@Test
	void resolveWithDockerConfigMetadataContextReturnsAddress() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-context/config.json"));
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("/home/user/.docker/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenEnvironmentHasAddressAndContextPrefersContext() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-context/config.json"));
		this.environment.put("DOCKER_CONTEXT", "test-context");
		this.environment.put("DOCKER_HOST", "notused");
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("/home/user/.docker/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	private String pathToResource(String resource) throws URISyntaxException {
		URL url = getClass().getResource(resource);
		return Paths.get(url.toURI()).getParent().toAbsolutePath().toString();
	}

}
