/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link RabbitConnectionDetails}
 * for a {@code rabbitmq} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jay Choi
 */
class RabbitStreamDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<RabbitStreamConnectionDetails> {

	protected RabbitStreamDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected @Nullable RabbitStreamConnectionDetails getDockerComposeConnectionDetails(
			DockerComposeConnectionSource source) {
		try {
			return new RabbitStreamDockerComposeConnectionDetails(source.getRunningService());
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * {@link RabbitStreamConnectionDetails} backed by a {@code rabbitmq}
	 * {@link RunningService}.
	 */
	static class RabbitStreamDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements RabbitStreamConnectionDetails {

		private static final int STREAMS_PORT = 5552;

		private static final int STREAMS_TLS_PORT = 5551;

		private final RabbitEnvironment environment;

		private final String host;

		private final int port;

		private final @Nullable SslBundle sslBundle;

		protected RabbitStreamDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new RabbitEnvironment(service.env());
			this.host = service.host();
			this.sslBundle = getSslBundle(service);
			this.port = service.ports().get((this.sslBundle != null) ? STREAMS_TLS_PORT : STREAMS_PORT);
		}

		@Override
		public @Nullable String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public String getVirtualHost() {
			return "/";
		}

		@Override
		public String getHost() {
			return this.host;
		}

		@Override
		public int getPort() {
			return this.port;
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

	}

}
