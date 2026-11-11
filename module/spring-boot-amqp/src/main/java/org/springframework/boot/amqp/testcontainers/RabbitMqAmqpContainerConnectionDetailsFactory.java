/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.testcontainers;

import java.net.URI;

import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link RabbitMQContainer}.
 *
 * @author Stephane Nicoll
 */
class RabbitMqAmqpContainerConnectionDetailsFactory
		extends AbstractAmqpContainerConnectionDetailsFactory<RabbitMQContainer> {

	@Override
	protected AmqpConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
		return new RabbitMqAmqpContainerConnectionDetails(source);
	}

	/**
	 * {@link AmqpConnectionDetails} for RabbitMQ backed by a
	 * {@link ContainerConnectionSource}.
	 */
	static final class RabbitMqAmqpContainerConnectionDetails
			extends AmqpContainerConnectionDetails<RabbitMQContainer> {

		private RabbitMqAmqpContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
			super(source);
		}

		@Override
		public Address getAddress() {
			URI uri = URI.create(getContainer().getAmqpUrl());
			return new Address(uri.getHost(), uri.getPort());
		}

		@Override
		public String getUsername() {
			return getContainer().getAdminUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getAdminPassword();
		}

	}

}
