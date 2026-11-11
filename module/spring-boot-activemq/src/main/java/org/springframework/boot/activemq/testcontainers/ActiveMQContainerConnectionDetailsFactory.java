/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ActiveMQConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer}
 * using the {@code "symptoma/activemq"} image.
 *
 * @author Eddú Meléndez
 */
class ActiveMQContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, ActiveMQConnectionDetails> {

	ActiveMQContainerConnectionDetailsFactory() {
		super("symptoma/activemq");
	}

	@Override
	protected ActiveMQConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new ActiveMQContainerConnectionDetails(source);
	}

	private static final class ActiveMQContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements ActiveMQConnectionDetails {

		private ActiveMQContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getBrokerUrl() {
			return "tcp://" + getContainer().getHost() + ":" + getContainer().getFirstMappedPort();
		}

		@Override
		public @Nullable String getUser() {
			return getContainer().getEnvMap().get("ACTIVEMQ_USERNAME");
		}

		@Override
		public @Nullable String getPassword() {
			return getContainer().getEnvMap().get("ACTIVEMQ_PASSWORD");
		}

	}

}
