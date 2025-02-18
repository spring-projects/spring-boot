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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.ExposedPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCliInspectResponse}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCliInspectResponseTests {

	@Test
	void deserializeJson() throws IOException {
		String json = new ClassPathResource("docker-inspect.json", getClass())
			.getContentAsString(StandardCharsets.UTF_8);
		DockerCliInspectResponse response = DockerJson.deserialize(json, DockerCliInspectResponse.class);
		LinkedHashMap<String, String> expectedLabels = linkedMapOf("com.docker.compose.config-hash",
				"cfdc8e119d85a53c7d47edb37a3b160a8c83ba48b0428ebc07713befec991dd0",
				"com.docker.compose.container-number", "1", "com.docker.compose.depends_on", "",
				"com.docker.compose.image", "sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82",
				"com.docker.compose.oneoff", "False", "com.docker.compose.project", "redis-docker",
				"com.docker.compose.project.config_files", "compose.yaml", "com.docker.compose.project.working_dir",
				"/", "com.docker.compose.service", "redis", "com.docker.compose.version", "2.16.0");
		List<String> expectedEnv = List.of("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
				"GOSU_VERSION=1.16", "REDIS_VERSION=7.0.8");
		Config expectedConfig = new Config("redis:7.0", expectedLabels, Map.of("6379/tcp", new ExposedPort()),
				expectedEnv);
		NetworkSettings expectedNetworkSettings = new NetworkSettings(
				Map.of("6379/tcp", List.of(new HostPort("0.0.0.0", "32770"), new HostPort("::", "32770"))));
		DockerCliInspectResponse expected = new DockerCliInspectResponse(
				"f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc", expectedConfig,
				expectedNetworkSettings, new HostConfig("redis-docker_default"));
		assertThat(response).isEqualTo(expected);
	}

	@SuppressWarnings("unchecked")
	private <K, V> LinkedHashMap<K, V> linkedMapOf(Object... values) {
		LinkedHashMap<K, V> result = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i = i + 2) {
			result.put((K) values[i], (V) values[i + 1]);
		}
		return result;
	}

}
