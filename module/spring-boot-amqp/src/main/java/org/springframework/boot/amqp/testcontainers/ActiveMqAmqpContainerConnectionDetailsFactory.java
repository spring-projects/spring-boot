/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.activemq.ActiveMQContainer;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link ActiveMQContainer}.
 *
 * @author Stephane Nicoll
 */
class ActiveMqAmqpContainerConnectionDetailsFactory
		extends AbstractAmqpContainerConnectionDetailsFactory<ActiveMQContainer> {

	@Override
	protected @Nullable AmqpConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ActiveMQContainer> source) {
		return new ActiveMqAmqpContainerConnectionDetails(source);
	}

	static class ActiveMqAmqpContainerConnectionDetails extends AmqpContainerConnectionDetails<ActiveMQContainer> {

		ActiveMqAmqpContainerConnectionDetails(ContainerConnectionSource<ActiveMQContainer> source) {
			super(source);
		}

		@Override
		public @Nullable String getUsername() {
			return getContainer().getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return getContainer().getPassword();
		}

	}

}
