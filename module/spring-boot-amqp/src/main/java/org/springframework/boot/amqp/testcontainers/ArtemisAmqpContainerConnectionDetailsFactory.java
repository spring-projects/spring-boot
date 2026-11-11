/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.activemq.ArtemisContainer;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link ArtemisContainer}.
 *
 * @author Stephane Nicoll
 */
class ArtemisAmqpContainerConnectionDetailsFactory
		extends AbstractAmqpContainerConnectionDetailsFactory<ArtemisContainer> {

	@Override
	protected @Nullable AmqpConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ArtemisContainer> source) {
		return new ArtemisAmqpContainerConnectionDetails(source);
	}

	static class ArtemisAmqpContainerConnectionDetails extends AmqpContainerConnectionDetails<ArtemisContainer> {

		ArtemisAmqpContainerConnectionDetails(ContainerConnectionSource<ArtemisContainer> source) {
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
