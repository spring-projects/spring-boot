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

import java.util.List;
import java.util.Map;

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
		DockerCliInspectResponse.NetworkSettings networkSettings, DockerCliInspectResponse.HostConfig hostConfig) {

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
	 * A container's resources (cgroups config, ulimits, etc).
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
