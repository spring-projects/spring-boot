/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ArtemisConnectionDetails} for an {@code artemis} service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ArtemisDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ArtemisConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	protected ArtemisDockerComposeConnectionDetailsFactory() {
		super(new String[] { "apache/activemq-artemis", "apache/artemis" });
	}

	@Override
	protected ArtemisConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ArtemisDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ArtemisConnectionDetails} backed by a {@code artemis}
	 * {@link RunningService}.
	 */
	static class ArtemisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ArtemisConnectionDetails {

		private final ArtemisEnvironment environment;

		private final String brokerUrl;

		protected ArtemisDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ArtemisEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
		}

		@Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		@Override
		public @Nullable String getUser() {
			return this.environment.getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

	}

}
