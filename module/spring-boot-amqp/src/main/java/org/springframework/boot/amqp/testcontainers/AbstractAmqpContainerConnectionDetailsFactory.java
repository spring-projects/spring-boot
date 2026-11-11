/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.testcontainers;

import org.testcontainers.containers.Container;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Base {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated container that is
 * compliant with AMQP 1.0.
 *
 * @param <C> the container type
 * @author Stephane Nicoll
 */
abstract class AbstractAmqpContainerConnectionDetailsFactory<C extends Container<?>>
		extends ContainerConnectionDetailsFactory<C, AmqpConnectionDetails> {

	protected static class AmqpContainerConnectionDetails<C extends Container<?>> extends ContainerConnectionDetails<C>
			implements AmqpConnectionDetails {

		private static final int AMQP_PORT = 5672;

		protected AmqpContainerConnectionDetails(ContainerConnectionSource<C> source) {
			super(source);
		}

		@Override
		public Address getAddress() {
			return new Address(getContainer().getHost(), getContainer().getMappedPort(AMQP_PORT));
		}

	}

}
