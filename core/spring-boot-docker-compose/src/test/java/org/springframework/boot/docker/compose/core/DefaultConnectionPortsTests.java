/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DefaultConnectionPorts.ContainerPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
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
 * @author Daniel Lykov
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

	@Test
	@SuppressWarnings("removal")
	void getWhenPortIsPublishedForBothProtocolsThrowsException() {
		Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
		mappings.put("53/tcp", List.of(new HostPort("127.0.0.1", "10053")));
		mappings.put("53/udp", List.of(new HostPort("127.0.0.1", "20053")));
		DefaultConnectionPorts ports = createForPorts(mappings);
		assertThatIllegalStateException().isThrownBy(() -> ports.get(53))
			.withMessage("Multiple host port mapping found for container port 53 with different protocols [tcp, udp]");
	}

	@Test
	@SuppressWarnings("removal")
	void getWhenPortIsOnlyUdpReturnsPort() {
		Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
		mappings.put("53/udp", List.of(new HostPort("127.0.0.1", "20053")));
		DefaultConnectionPorts ports = createForPorts(mappings);
		assertThat(ports.get(53)).isEqualTo(20053);
		assertThat(ports.getAll("udp")).containsExactly(20053);
	}

	@Test
	@SuppressWarnings("removal")
	void getWhenPortIsOnlyTcpReturnsPort() {
		Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
		mappings.put("53/tcp", List.of(new HostPort("127.0.0.1", "10053")));
		DefaultConnectionPorts ports = createForPorts(mappings);
		assertThat(ports.get(53)).isEqualTo(10053);
		assertThat(ports.getAll("tcp")).containsExactly(10053);
	}

	@Test
	void getTcpWhenPortIsPublishedForBothProtocolsReturnsTcp() {
		Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
		mappings.put("53/tcp", List.of(new HostPort("127.0.0.1", "10053")));
		mappings.put("53/udp", List.of(new HostPort("127.0.0.1", "20053")));
		DefaultConnectionPorts ports = createForPorts(mappings);
		assertThat(ports.getTcp(53)).isEqualTo(10053);
		assertThat(ports.getAll("udp")).containsExactly(20053);
	}

	@Test
	void getUdpWhenPortIsPublishedForBothProtocolsReturnsUdp() {
		Map<String, List<HostPort>> mappings = new LinkedHashMap<>();
		mappings.put("53/tcp", List.of(new HostPort("127.0.0.1", "10053")));
		mappings.put("53/udp", List.of(new HostPort("127.0.0.1", "20053")));
		DefaultConnectionPorts ports = createForPorts(mappings);
		assertThat(ports.getUdp(53)).isEqualTo(20053);
		assertThat(ports.getAll("udp")).containsExactly(20053);
	}

	@Test
	void getUdpReturnsUdpPortWhenNoTcpPortIsPublished() {
		DefaultConnectionPorts ports = createForPorts(Map.of("53/udp", List.of(new HostPort("127.0.0.1", "20053"))));
		assertThat(ports.getUdp(53)).isEqualTo(20053);
	}

	private DefaultConnectionPorts createForPorts(Map<String, List<HostPort>> mappings) {
		DockerCliInspectResponse response = new DockerCliInspectResponse("id",
				new Config("test", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()),
				new NetworkSettings(mappings), null);
		return new DefaultConnectionPorts(response);
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
