/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.testcontainers;

import java.net.URI;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.rabbitmq.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link RabbitConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link RabbitMQContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RabbitMqContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RabbitMQContainer, RabbitConnectionDetails> {

	@Override
	protected RabbitConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<RabbitMQContainer> source) {
		return new RabbitMqContainerConnectionDetails(source);
	}

	/**
	 * {@link RabbitConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	static final class RabbitMqContainerConnectionDetails extends ContainerConnectionDetails<RabbitMQContainer>
			implements RabbitConnectionDetails {

		private RabbitMqContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
			super(source);
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
		public List<Address> getAddresses() {
			URI uri = URI.create((getSslBundle() != null) ? getContainer().getAmqpsUrl() : getContainer().getAmqpUrl());
			return List.of(new Address(uri.getHost(), uri.getPort()));
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
