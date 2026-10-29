/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.docker.compose;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.mail.autoconfigure.MailConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link MailConnectionDetails}
 * for an {@code axllent/mailpit} service using its default SMTP configuration.
 *
 * @author Dominik Kovacs
 */
class MailpitDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<MailConnectionDetails> {

	private static final int SMTP_PORT = 1025;

	MailpitDockerComposeConnectionDetailsFactory() {
		super("axllent/mailpit", "org.springframework.mail.javamail.JavaMailSender");
	}

	@Override
	protected MailConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new MailpitDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link MailConnectionDetails} backed by an {@code axllent/mailpit}
	 * {@link RunningService}.
	 */
	static class MailpitDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements MailConnectionDetails {

		private final String host;

		private final int port;

		MailpitDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.host = service.host();
			this.port = service.ports().get(SMTP_PORT);
		}

		@Override
		public String getHost() {
			return this.host;
		}

		@Override
		public int getPort() {
			return this.port;
		}

	}

}
