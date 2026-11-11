/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Response from {@link DockerCliCommand.Inspect docker inspect}.
 *
 * @param id the container id
 * @param config the config
 * @param hostConfig the host config
 * @param networkSettings the network settings
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliInspectResponse(String id, DockerCliInspectResponse.Config config,
		DockerCliInspectResponse.@Nullable NetworkSettings networkSettings,
		DockerCliInspectResponse.@Nullable HostConfig hostConfig) {

	/**
	 * Configuration for the container that is portable between hosts.
	 *
	 * @param image the name (or reference) of the image
	 * @param labels user-defined key/value metadata
	 * @param exposedPorts the mapping of exposed ports
	 * @param env a list of environment variables in the form {@code VAR=value}
	 */
	record Config(String image, Map<String, String> labels, Map<String, ExposedPort> exposedPorts, List<String> env) {

	}

	/**
	 * Empty object used with {@link Config#exposedPorts()}.
	 */
	record ExposedPort() {

	}

	/**
	 * A container's resources (cgroups config, ulimits, etc.).
	 *
	 * @param networkMode the network mode to use for this container
	 */
	record HostConfig(String networkMode) {

	}

	/**
	 * The network settings in the API.
	 *
	 * @param ports the mapping of container ports to host ports
	 */
	record NetworkSettings(Map<String, List<HostPort>> ports) {

	}

	/**
	 * Port mapping details.
	 *
	 * @param hostIp the host IP
	 * @param hostPort the host port
	 */
	record HostPort(String hostIp, String hostPort) {

	}

}
