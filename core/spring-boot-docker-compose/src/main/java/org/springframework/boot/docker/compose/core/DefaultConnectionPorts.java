/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Default {@link ConnectionPorts} implementation backed by {@link DockerCli} responses.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Daniel Lykov
 */
class DefaultConnectionPorts implements ConnectionPorts {

	private final Map<ContainerPort, Integer> mappings;

	private final MultiValueMap<Integer, String> protocols;

	DefaultConnectionPorts(DockerCliInspectResponse inspectResponse) {
		this.mappings = !isHostNetworkMode(inspectResponse)
				? buildMappingsForNetworkSettings(inspectResponse.networkSettings())
				: buildMappingsForHostNetworking(inspectResponse.config());
		this.protocols = buildProtocolMappings(this.mappings.keySet());
	}

	private static boolean isHostNetworkMode(DockerCliInspectResponse inspectResponse) {
		HostConfig config = inspectResponse.hostConfig();
		return (config != null) && "host".equals(config.networkMode());
	}

	private Map<ContainerPort, Integer> buildMappingsForNetworkSettings(@Nullable NetworkSettings networkSettings) {
		if (networkSettings == null || CollectionUtils.isEmpty(networkSettings.ports())) {
			return Collections.emptyMap();
		}
		Map<ContainerPort, Integer> mappings = new HashMap<>();
		networkSettings.ports().forEach((containerPortString, hostPorts) -> {
			if (!CollectionUtils.isEmpty(hostPorts)) {
				ContainerPort containerPort = ContainerPort.parse(containerPortString);
				hostPorts.stream()
					.filter(this::isIpV4)
					.forEach((hostPort) -> mappings.put(containerPort, getPortNumber(hostPort)));
			}
		});
		return Collections.unmodifiableMap(mappings);
	}

	private boolean isIpV4(@Nullable HostPort hostPort) {
		String ip = (hostPort != null) ? hostPort.hostIp() : null;
		return !StringUtils.hasLength(ip) || ip.contains(".");
	}

	private int getPortNumber(HostPort hostPort) {
		return Integer.parseInt(hostPort.hostPort());
	}

	private Map<ContainerPort, Integer> buildMappingsForHostNetworking(Config config) {
		if (CollectionUtils.isEmpty(config.exposedPorts())) {
			return Collections.emptyMap();
		}
		Map<ContainerPort, Integer> mappings = new HashMap<>();
		for (String entry : config.exposedPorts().keySet()) {
			ContainerPort containerPort = ContainerPort.parse(entry);
			mappings.put(containerPort, containerPort.number());
		}
		return Collections.unmodifiableMap(mappings);
	}

	private MultiValueMap<Integer, String> buildProtocolMappings(Set<ContainerPort> containerPorts) {
		MultiValueMap<Integer, String> protocols = new LinkedMultiValueMap<>();
		containerPorts.forEach((containerPort) -> protocols.add(containerPort.number(), containerPort.protocol()));
		return CollectionUtils.unmodifiableMultiValueMap(protocols);
	}

	@Override
	@SuppressWarnings("removal")
	public int get(int containerPort) {
		List<String> protocols = this.protocols.get(containerPort);
		Assert.state(protocols != null,
				() -> "No host port mapping found for container port %s".formatted(containerPort));
		Assert.state(protocols.size() == 1,
				() -> "Multiple host port mapping found for container port %s with different protocols %s"
					.formatted(containerPort, protocols));
		return get(containerPort, protocols.get(0));
	}

	@Override
	public int get(int containerPort, String protocol) {
		Assert.hasText(protocol, "'protocol' must not be empty");
		Integer hostPort = this.mappings.get(new ContainerPort(containerPort, protocol));
		Assert.state(hostPort != null,
				() -> "No host port mapping found for container port %s".formatted(containerPort));
		return hostPort;
	}

	@Override
	public List<Integer> getAll() {
		return getAll(null);
	}

	@Override
	public List<Integer> getAll(@Nullable String protocol) {
		List<Integer> hostPorts = new ArrayList<>();
		this.mappings.forEach((containerPort, hostPort) -> {
			if (protocol == null || protocol.equalsIgnoreCase(containerPort.protocol())) {
				hostPorts.add(hostPort);
			}
		});
		return Collections.unmodifiableList(hostPorts);
	}

	Map<ContainerPort, Integer> getMappings() {
		return this.mappings;
	}

	/**
	 * A container port consisting of a number and protocol.
	 *
	 * @param number the port number
	 * @param protocol the protocol (e.g. tcp)
	 */
	record ContainerPort(int number, String protocol) {

		@Override
		public String toString() {
			return "%d/%s".formatted(this.number, this.protocol);
		}

		static ContainerPort parse(String value) {
			try {
				String[] parts = value.split("/");
				Assert.state(parts.length == 2, "Unable to split string");
				return new ContainerPort(Integer.parseInt(parts[0]), parts[1]);
			}
			catch (RuntimeException ex) {
				throw new IllegalStateException("Unable to parse container port '%s'".formatted(value), ex);
			}
		}

	}

}
