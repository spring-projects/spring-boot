/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testcontainers.service.connection.pulsar;

import org.testcontainers.containers.PulsarContainer;

import org.springframework.boot.autoconfigure.pulsar.PulsarConnectionDetails;
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

	/**
	 * Returns the connection details for the Pulsar container.
	 * @param source the source of the container connection
	 * @return the connection details for the Pulsar container
	 */
	@Override
	protected PulsarConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<PulsarContainer> source) {
		return new PulsarContainerConnectionDetails(source);
	}

	/**
	 * {@link PulsarConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class PulsarContainerConnectionDetails extends ContainerConnectionDetails<PulsarContainer>
			implements PulsarConnectionDetails {

		/**
		 * Constructs a new PulsarContainerConnectionDetails object with the specified
		 * source.
		 * @param source the source of the container connection details
		 */
		private PulsarContainerConnectionDetails(ContainerConnectionSource<PulsarContainer> source) {
			super(source);
		}

		/**
		 * Returns the broker URL for the Pulsar container connection details.
		 * @return the broker URL
		 */
		@Override
		public String getBrokerUrl() {
			return getContainer().getPulsarBrokerUrl();
		}

		/**
		 * Returns the URL of the admin interface for the Pulsar container.
		 * @return the URL of the admin interface
		 */
		@Override
		public String getAdminUrl() {
			return getContainer().getHttpServiceUrl();
		}

	}

}
