/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.rabbitmq.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link RabbitStreamConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link RabbitMQContainer}.
 *
 * @author Eddú Meléndez
 * @author Jay Choi
 */
class RabbitMqStreamContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RabbitMQContainer, RabbitStreamConnectionDetails> {

	RabbitMqStreamContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "org.springframework.rabbit.stream.producer.RabbitStreamTemplate");
	}

	@Override
	protected boolean sourceAccepts(ContainerConnectionSource<RabbitMQContainer> source, Class<?> requiredContainerType,
			Class<?> requiredConnectionDetailsType) {
		return source.getConnectionDetailsTypes().contains(requiredConnectionDetailsType)
				&& super.sourceAccepts(source, requiredContainerType, requiredConnectionDetailsType);
	}

	@Override
	protected RabbitStreamConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<RabbitMQContainer> source) {
		return new RabbitMqStreamContainerConnectionDetails(source);
	}

	/**
	 * {@link RabbitStreamConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	static final class RabbitMqStreamContainerConnectionDetails extends ContainerConnectionDetails<RabbitMQContainer>
			implements RabbitStreamConnectionDetails {

		private static final int STREAMS_PORT = 5552;

		private static final int STREAMS_TLS_PORT = 5551;

		private RabbitMqStreamContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
			super(source);
		}

		@Override
		public String getHost() {
			return getContainer().getHost();
		}

		@Override
		public int getPort() {
			return getContainer().getMappedPort((getSslBundle() != null) ? STREAMS_TLS_PORT : STREAMS_PORT);
		}

		@Override
		public String getUsername() {
			return getContainer().getAdminUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getAdminPassword();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
