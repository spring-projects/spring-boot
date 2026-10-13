/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.docker.compose;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link PulsarConnectionDetails}
 * for a {@code pulsar} service.
 *
 * @author Chris Bono
 */
class PulsarDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<PulsarConnectionDetails> {

	private static final int BROKER_PORT = 6650;

	private static final int ADMIN_PORT = 8080;

	PulsarDockerComposeConnectionDetailsFactory() {
		super("apachepulsar/pulsar");
	}

	@Override
	protected PulsarConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PulsarDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link PulsarConnectionDetails} backed by a {@code pulsar} {@link RunningService}.
	 */
	static class PulsarDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements PulsarConnectionDetails {

		private final String brokerUrl;

		private final String adminUrl;

		PulsarDockerComposeConnectionDetails(RunningService service) {
			super(service);
			ConnectionPorts ports = service.ports();
			this.brokerUrl = "pulsar://%s:%s".formatted(service.host(), ports.get(BROKER_PORT));
			this.adminUrl = "http://%s:%s".formatted(service.host(), ports.get(ADMIN_PORT));
		}

		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		@Override
		public String getAdminUrl() {
			return this.adminUrl;
		}

	}

}
