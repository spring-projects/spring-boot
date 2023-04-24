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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.ExposedPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
import org.springframework.boot.origin.Origin;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DefaultRunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultRunningServiceTests {

	@TempDir
	File temp;

	private DefaultRunningService runningService;

	private DockerComposeFile composeFile;

	@BeforeEach
	void setup() throws Exception {
		this.composeFile = createComposeFile();
		this.runningService = createRunningService(true);
	}

	private DockerComposeFile createComposeFile() throws IOException {
		File file = new File(this.temp, "compose.yaml");
		FileCopyUtils.copy(new byte[0], file);
		return DockerComposeFile.of(file);
	}

	@Test
	void getOriginReturnsOrigin() {
		assertThat(Origin.from(this.runningService)).isEqualTo(new DockerComposeOrigin(this.composeFile, "my-service"));
	}

	@Test
	void nameReturnsNameFromPsResponse() {
		assertThat(this.runningService.name()).isEqualTo("my-service");
	}

	@Test
	void imageReturnsImageFromPsResponse() {
		assertThat(this.runningService.image()).hasToString("docker.io/library/redis");
	}

	@Test // gh-34992
	void imageWhenUsingEarlierDockerVersionReturnsImageFromInspectResult() {
		DefaultRunningService runningService = createRunningService(false);
		assertThat(runningService.image()).hasToString("docker.io/library/redis");

	}

	@Test
	void hostReturnsHost() {
		assertThat(this.runningService.host()).isEqualTo("192.168.1.1");
	}

	@Test
	void portsReturnsPortsFromInspectResponse() {
		ConnectionPorts ports = this.runningService.ports();
		assertThat(ports.getAll("tcp")).containsExactly(9090);
		assertThat(ports.get(8080)).isEqualTo(9090);
	}

	@Test
	void envReturnsEnvFromInspectResponse() {
		assertThat(this.runningService.env()).containsExactly(entry("a", "b"));
	}

	@Test
	void labelReturnsLabelsFromInspectResponse() {
		assertThat(this.runningService.labels()).containsExactly(entry("spring", "boot"));
	}

	@Test
	void toStringReturnsServiceName() {
		assertThat(this.runningService).hasToString("my-service");
	}

	private DefaultRunningService createRunningService(boolean psResponseHasImage) {
		DockerHost host = DockerHost.get("192.168.1.1", () -> Collections.emptyList());
		String id = "123";
		String name = "my-service";
		String image = "redis";
		String state = "running";
		DockerCliComposePsResponse psResponse = new DockerCliComposePsResponse(id, name,
				(!psResponseHasImage) ? null : image, state);
		Map<String, String> labels = Map.of("spring", "boot");
		Map<String, ExposedPort> exposedPorts = Map.of("8080/tcp", new ExposedPort());
		List<String> env = List.of("a=b");
		Config config = new Config(image, labels, exposedPorts, env);
		Map<String, List<HostPort>> ports = Map.of("8080/tcp", List.of(new HostPort(null, "9090")));
		NetworkSettings networkSettings = new NetworkSettings(ports);
		HostConfig hostConfig = new HostConfig("bridge");
		DockerCliInspectResponse inspectResponse = new DockerCliInspectResponse(id, config, networkSettings,
				hostConfig);
		return new DefaultRunningService(host, this.composeFile, psResponse, inspectResponse);
	}

}
