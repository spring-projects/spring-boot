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

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public ImageReference image() {
		return this.image;
	}

	@Override
	public String host() {
		return this.host.toString();
	}

	@Override
	public ConnectionPorts ports() {
		return this.ports;
	}

	@Override
	public Map<String, String> env() {
		return this.env.asMap();
	}

	@Override
	public Map<String, String> labels() {
		return this.labels;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
