/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.testcontainers;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.mail.autoconfigure.MailConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} for a {@link ServiceConnection}-annotated
 * {@link GenericContainer} using the {@code "axllent/mailpit"} image with its default
 * SMTP configuration.
 *
 * @author Van Vo
 */
class MailpitContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, MailConnectionDetails> {

	private static final int SMTP_PORT = 1025;

	MailpitContainerConnectionDetailsFactory() {
		super("axllent/mailpit", "org.springframework.mail.javamail.JavaMailSender");
	}

	@Override
	protected MailConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new MailpitContainerConnectionDetails(source);
	}

	private static class MailpitContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements MailConnectionDetails {

		MailpitContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getHost() {
			return getContainer().getHost();
		}

		@Override
		public int getPort() {
			return getContainer().getMappedPort(SMTP_PORT);
		}

	}

}
