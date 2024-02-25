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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link ConnectionPorts} implementation backed by {@link DockerCli} responses.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultConnectionPorts implements ConnectionPorts {

	private final Map<ContainerPort, Integer> mappings;

	private final Map<Integer, Integer> portMappings;

	/**
     * Builds the default connection ports based on the Docker CLI inspect response.
     * 
     * @param inspectResponse the Docker CLI inspect response
     */
    DefaultConnectionPorts(DockerCliInspectResponse inspectResponse) {
		this.mappings = !isHostNetworkMode(inspectResponse)
				? buildMappingsForNetworkSettings(inspectResponse.networkSettings())
				: buildMappingsForHostNetworking(inspectResponse.config());
		Map<Integer, Integer> portMappings = new HashMap<>();
		this.mappings.forEach((containerPort, hostPort) -> portMappings.put(containerPort.number(), hostPort));
		this.portMappings = Collections.unmodifiableMap(portMappings);
	}

	/**
     * Checks if the network mode of the Docker container is set to "host".
     * 
     * @param inspectResponse the Docker CLI inspect response object
     * @return true if the network mode is set to "host", false otherwise
     */
    private static boolean isHostNetworkMode(DockerCliInspectResponse inspectResponse) {
		HostConfig config = inspectResponse.hostConfig();
		return (config != null) && "host".equals(config.networkMode());
	}

	/**
     * Builds mappings for network settings.
     * 
     * @param networkSettings the network settings to build mappings for
     * @return a map of container ports to host ports
     */
    private Map<ContainerPort, Integer> buildMappingsForNetworkSettings(NetworkSettings networkSettings) {
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

	/**
     * Checks if the given HostPort object represents an IPv4 address.
     * 
     * @param hostPort the HostPort object to check
     * @return true if the HostPort object represents an IPv4 address, false otherwise
     */
    private boolean isIpV4(HostPort hostPort) {
		String ip = (hostPort != null) ? hostPort.hostIp() : null;
		return !StringUtils.hasLength(ip) || ip.contains(".");
	}

	/**
     * Returns the port number from the given HostPort object.
     * 
     * @param hostPort the HostPort object containing the host and port information
     * @return the port number extracted from the HostPort object
     * @throws NumberFormatException if the port number cannot be parsed as an integer
     */
    private static int getPortNumber(HostPort hostPort) {
		return Integer.parseInt(hostPort.hostPort());
	}

	/**
     * Builds mappings for host networking based on the provided configuration.
     * 
     * @param config the configuration object containing the exposed ports
     * @return a map of container ports to their corresponding host ports
     */
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

	/**
     * Retrieves the host port corresponding to the given container port.
     * 
     * @param containerPort the container port for which the host port is to be retrieved
     * @return the host port corresponding to the container port
     * @throws IllegalStateException if no host port mapping is found for the container port
     */
    @Override
	public int get(int containerPort) {
		Integer hostPort = this.portMappings.get(containerPort);
		Assert.state(hostPort != null, "No host port mapping found for container port %s".formatted(containerPort));
		return hostPort;
	}

	/**
     * Retrieves a list of all connection ports.
     * 
     * @return a list of integers representing the connection ports
     */
    @Override
	public List<Integer> getAll() {
		return getAll(null);
	}

	/**
     * Retrieves a list of all host ports based on the specified protocol.
     * 
     * @param protocol the protocol to filter the host ports by (optional)
     * @return a list of host ports that match the specified protocol
     */
    @Override
	public List<Integer> getAll(String protocol) {
		List<Integer> hostPorts = new ArrayList<>();
		this.mappings.forEach((containerPort, hostPort) -> {
			if (protocol == null || protocol.equalsIgnoreCase(containerPort.protocol())) {
				hostPorts.add(hostPort);
			}
		});
		return Collections.unmodifiableList(hostPorts);
	}

	/**
     * Returns the mappings of container ports to integers.
     *
     * @return the mappings of container ports to integers
     */
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

		/**
     * Returns a string representation of the DefaultConnectionPorts object.
     * The string representation is in the format of "%d/%s", where %d represents the number
     * and %s represents the protocol.
     *
     * @return a string representation of the DefaultConnectionPorts object
     */
    @Override
		public String toString() {
			return "%d/%s".formatted(this.number, this.protocol);
		}

		/**
     * Parses a string representation of a container port into a ContainerPort object.
     * The string should be in the format "port/protocol".
     * 
     * @param value the string representation of the container port
     * @return the parsed ContainerPort object
     * @throws IllegalStateException if the string cannot be parsed
     */
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
