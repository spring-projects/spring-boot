/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.testcontainers;

import org.testcontainers.pulsar.PulsarContainer;

import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link PulsarConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link PulsarContainer}.
 *
 * @author Chris Bono
 */
class PulsarContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<PulsarContainer, PulsarConnectionDetails> {

	@Override
	protected PulsarConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<PulsarContainer> source) {
		return new PulsarContainerConnectionDetails(source);
	}

	/**
	 * {@link PulsarConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class PulsarContainerConnectionDetails extends ContainerConnectionDetails<PulsarContainer>
			implements PulsarConnectionDetails {

		private PulsarContainerConnectionDetails(ContainerConnectionSource<PulsarContainer> source) {
			super(source);
		}

		@Override
		public String getBrokerUrl() {
			return getContainer().getPulsarBrokerUrl();
		}

		@Override
		public String getAdminUrl() {
			return getContainer().getHttpServiceUrl();
		}

	}

}
