/*
 * Copyright 2012-2022 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

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
	void resolveWhenDockerHostIsNullReturnsLinuxDefault() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("/var/run/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void resolveWhenDockerHostIsNullReturnsWindowsDefault() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, null);
		assertThat(dockerHost.getAddress()).isEqualTo("//./pipe/docker_engine");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void resolveWhenDockerHostAddressIsNullReturnsLinuxDefault() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get, new DockerHost(null));
		assertThat(dockerHost.getAddress()).isEqualTo("/var/run/docker.sock");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsLocalReturnsAddress(@TempDir Path tempDir) throws IOException {
		String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				new DockerHost(socketFilePath, false, null));
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
				new DockerHost("unix://" + socketFilePath, false, null));
		assertThat(dockerHost.isLocalFileReference()).isTrue();
		assertThat(dockerHost.isRemote()).isFalse();
		assertThat(dockerHost.getAddress()).isEqualTo(socketFilePath);
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsHttpReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				new DockerHost("http://docker.example.com", false, null));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("http://docker.example.com");
		assertThat(dockerHost.isSecure()).isFalse();
		assertThat(dockerHost.getCertificatePath()).isNull();
	}

	@Test
	void resolveWhenDockerHostAddressIsHttpsReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				new DockerHost("https://docker.example.com", true, "/cert-path"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("https://docker.example.com");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isEqualTo("/cert-path");
	}

	@Test
	void resolveWhenDockerHostAddressIsTcpReturnsAddress() {
		ResolvedDockerHost dockerHost = ResolvedDockerHost.from(this.environment::get,
				new DockerHost("tcp://192.168.99.100:2376", true, "/cert-path"));
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
				new DockerHost("/unused", true, "/unused"));
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
				new DockerHost("/unused", true, "/unused"));
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
				new DockerHost("tcp://1.1.1.1", false, "/unused"));
		assertThat(dockerHost.isLocalFileReference()).isFalse();
		assertThat(dockerHost.isRemote()).isTrue();
		assertThat(dockerHost.getAddress()).isEqualTo("tcp://192.168.99.100:2376");
		assertThat(dockerHost.isSecure()).isTrue();
		assertThat(dockerHost.getCertificatePath()).isEqualTo("/cert-path");
	}

}
