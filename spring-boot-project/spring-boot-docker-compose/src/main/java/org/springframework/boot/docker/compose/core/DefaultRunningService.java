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

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;

/**
 * Default {@link RunningService} implementation backed by {@link DockerCli} responses.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultRunningService implements RunningService, OriginProvider {

	private final Origin origin;

	private final String name;

	private final ImageReference image;

	private final DockerHost host;

	private final DefaultConnectionPorts ports;

	private final Map<String, String> labels;

	private final DockerEnv env;

	/**
	 * Creates a new instance of DefaultRunningService.
	 * @param host the DockerHost where the service is running
	 * @param composeFile the DockerComposeFile used to deploy the service
	 * @param composePsResponse the DockerCliComposePsResponse containing information
	 * about the service
	 * @param inspectResponse the DockerCliInspectResponse containing detailed information
	 * about the service
	 */
	DefaultRunningService(DockerHost host, DockerComposeFile composeFile, DockerCliComposePsResponse composePsResponse,
			DockerCliInspectResponse inspectResponse) {
		this.origin = new DockerComposeOrigin(composeFile, composePsResponse.name());
		this.name = composePsResponse.name();
		this.image = ImageReference
			.of((composePsResponse.image() != null) ? composePsResponse.image() : inspectResponse.config().image());
		this.host = host;
		this.ports = new DefaultConnectionPorts(inspectResponse);
		this.env = new DockerEnv(inspectResponse.config().env());
		this.labels = Collections.unmodifiableMap(inspectResponse.config().labels());
	}

	/**
	 * Returns the origin of the running service.
	 * @return the origin of the running service
	 */
	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	/**
	 * Returns the name of the running service.
	 * @return the name of the running service
	 */
	@Override
	public String name() {
		return this.name;
	}

	/**
	 * Returns the image reference associated with this running service.
	 * @return the image reference
	 */
	@Override
	public ImageReference image() {
		return this.image;
	}

	/**
	 * Returns the host of the running service.
	 * @return the host of the running service
	 */
	@Override
	public String host() {
		return this.host.toString();
	}

	/**
	 * Returns the connection ports of the running service.
	 * @return the connection ports of the running service
	 */
	@Override
	public ConnectionPorts ports() {
		return this.ports;
	}

	/**
	 * Returns the environment variables as a map.
	 * @return a map containing the environment variables
	 */
	@Override
	public Map<String, String> env() {
		return this.env.asMap();
	}

	/**
	 * Returns the labels associated with this running service.
	 * @return a map containing the labels as key-value pairs
	 */
	@Override
	public Map<String, String> labels() {
		return this.labels;
	}

	/**
	 * Returns a string representation of the object.
	 * @return the name of the object
	 */
	@Override
	public String toString() {
		return this.name;
	}

}
