/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.activemq.ArtemisContainer;

import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ArtemisConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link ArtemisContainer}.
 *
 * @author Eddú Meléndez
 */
class ArtemisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ArtemisContainer, ArtemisConnectionDetails> {

	@Override
	protected ArtemisConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ArtemisContainer> source) {
		return new ArtemisContainerConnectionDetails(source);
	}

	private static final class ArtemisContainerConnectionDetails extends ContainerConnectionDetails<ArtemisContainer>
			implements ArtemisConnectionDetails {

		private ArtemisContainerConnectionDetails(ContainerConnectionSource<ArtemisContainer> source) {
			super(source);
		}

		@Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		@Override
		public String getBrokerUrl() {
			return getContainer().getBrokerUrl();
		}

		@Override
		public @Nullable String getUser() {
			return getContainer().getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return getContainer().getPassword();
		}

	}

}
