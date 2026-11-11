/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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

	private final @Nullable DockerComposeFile composeFile;

	DefaultRunningService(DockerHost host, @Nullable DockerComposeFile composeFile,
			DockerCliComposePsResponse composePsResponse, DockerCliInspectResponse inspectResponse) {
		this.origin = new DockerComposeOrigin(composeFile, composePsResponse.name());
		this.name = composePsResponse.name();
		this.image = ImageReference
			.of((composePsResponse.image() != null) ? composePsResponse.image() : inspectResponse.config().image());
		this.host = host;
		this.ports = new DefaultConnectionPorts(inspectResponse);
		this.env = new DockerEnv(inspectResponse.config().env());
		this.labels = Collections.unmodifiableMap(inspectResponse.config().labels());
		this.composeFile = composeFile;
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
	public Map<String, @Nullable String> env() {
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

	@Override
	public @Nullable DockerComposeFile composeFile() {
		return this.composeFile;
	}

}
