/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ActiveMQConnectionDetails} for an {@code activemq} service.
 *
 * @author Stephane Nicoll
 */
class ActiveMQDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ActiveMQConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	protected ActiveMQDockerComposeConnectionDetailsFactory() {
		super("symptoma/activemq");
	}

	@Override
	protected ActiveMQConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ActiveMQDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ActiveMQConnectionDetails} backed by an {@code activemq}
	 * {@link RunningService}.
	 */
	static class ActiveMQDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ActiveMQConnectionDetails {

		private final ActiveMQEnvironment environment;

		private final String brokerUrl;

		protected ActiveMQDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ActiveMQEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
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
