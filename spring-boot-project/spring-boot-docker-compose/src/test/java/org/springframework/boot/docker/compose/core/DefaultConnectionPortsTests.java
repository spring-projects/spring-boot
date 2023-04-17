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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DefaultConnectionPorts.ContainerPort;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DefaultConnectionPorts}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultConnectionPortsTests {

	@Test
	void createWhenBridgeNetwork() throws IOException {
		DefaultConnectionPorts ports = createForJson("docker-inspect-bridge-network.json");
		assertThat(ports.getMappings()).containsExactly(entry(new ContainerPort(6379, "tcp"), 32770));
	}

	@Test
	void createWhenHostNetwork() throws Exception {
		DefaultConnectionPorts ports = createForJson("docker-inspect-host-network.json");
		assertThat(ports.getMappings()).containsExactly(entry(new ContainerPort(6379, "tcp"), 6379));
	}

	private DefaultConnectionPorts createForJson(String path) throws IOException {
		String json = new ClassPathResource(path, getClass()).getContentAsString(StandardCharsets.UTF_8);
		DockerCliInspectResponse inspectResponse = DockerJson.deserialize(json, DockerCliInspectResponse.class);
		return new DefaultConnectionPorts(inspectResponse);
	}

	@Nested
	class ContainerPortTests {

		@Test
		void parse() {
			ContainerPort port = ContainerPort.parse("123/tcp");
			assertThat(port).isEqualTo(new ContainerPort(123, "tcp"));
		}

		@Test
		void parseWhenNoSlashThrowsException() {
			assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("123"))
				.withMessage("Unable to parse container port '123'");
		}

		@Test
		void parseWhenMultipleSlashesThrowsException() {
			assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("123/tcp/ip"))
				.withMessage("Unable to parse container port '123/tcp/ip'");
		}

		@Test
		void parseWhenNotNumberThrowsException() {
			assertThatIllegalStateException().isThrownBy(() -> ContainerPort.parse("tcp/123"))
				.withMessage("Unable to parse container port 'tcp/123'");
		}

	}

}
