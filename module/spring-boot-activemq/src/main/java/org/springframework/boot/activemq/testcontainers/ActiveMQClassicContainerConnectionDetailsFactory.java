/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.testcontainers;

import org.testcontainers.activemq.ActiveMQContainer;

import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ActiveMQConnectionDetails} *
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link ActiveMQContainer}.
 *
 * @author Eddú Meléndez
 */
class ActiveMQClassicContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ActiveMQContainer, ActiveMQConnectionDetails> {

	@Override
	protected ActiveMQConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ActiveMQContainer> source) {
		return new ActiveMQContainerConnectionDetails(source);
	}

	private static final class ActiveMQContainerConnectionDetails extends ContainerConnectionDetails<ActiveMQContainer>
			implements ActiveMQConnectionDetails {

		private ActiveMQContainerConnectionDetails(ContainerConnectionSource<ActiveMQContainer> source) {
			super(source);
		}

		@Override
		public String getBrokerUrl() {
			return getContainer().getBrokerUrl();
		}

		@Override
		public String getUser() {
			return getContainer().getUser();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

	}

}
