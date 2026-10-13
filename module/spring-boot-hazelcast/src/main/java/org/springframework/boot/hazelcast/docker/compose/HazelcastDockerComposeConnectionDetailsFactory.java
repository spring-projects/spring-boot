/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.docker.compose;

import com.hazelcast.client.config.ClientConfig;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link HazelcastConnectionDetails} for a {@code hazelcast} service.
 *
 * @author Dmytro Nosan
 */
class HazelcastDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<HazelcastConnectionDetails> {

	private static final int DEFAULT_PORT = 5701;

	protected HazelcastDockerComposeConnectionDetailsFactory() {
		super("hazelcast/hazelcast", "com.hazelcast.client.config.ClientConfig");
	}

	@Override
	protected HazelcastConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new HazelcastDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link HazelcastConnectionDetails} backed by a {@code hazelcast}
	 * {@link RunningService}.
	 */
	static class HazelcastDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements HazelcastConnectionDetails {

		private final String host;

		private final int port;

		private final HazelcastEnvironment environment;

		HazelcastDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.host = service.host();
			this.port = service.ports().get(DEFAULT_PORT);
			this.environment = new HazelcastEnvironment(service.env());
		}

		@Override
		public ClientConfig getClientConfig() {
			ClientConfig config = new ClientConfig();
			if (this.environment.getClusterName() != null) {
				config.setClusterName(this.environment.getClusterName());
			}
			config.getNetworkConfig().addAddress(this.host + ":" + this.port);
			return config;
		}

	}

}
