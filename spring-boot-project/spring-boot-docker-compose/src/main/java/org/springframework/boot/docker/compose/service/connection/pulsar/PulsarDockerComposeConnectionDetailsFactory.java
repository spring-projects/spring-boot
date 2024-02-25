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

package org.springframework.boot.docker.compose.service.connection.pulsar;

import org.springframework.boot.autoconfigure.pulsar.PulsarConnectionDetails;
import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link PulsarConnectionDetails}
 * for a {@code pulsar} service.
 *
 * @author Chris Bono
 */
class PulsarDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<PulsarConnectionDetails> {

	private static final int BROKER_PORT = 6650;

	private static final int ADMIN_PORT = 8080;

	/**
	 * Constructs a new PulsarDockerComposeConnectionDetailsFactory with the default
	 * Docker image "apachepulsar/pulsar".
	 */
	PulsarDockerComposeConnectionDetailsFactory() {
		super("apachepulsar/pulsar");
	}

	/**
	 * Returns the PulsarConnectionDetails object for the given
	 * DockerComposeConnectionSource.
	 * @param source the DockerComposeConnectionSource object containing the running
	 * service information
	 * @return the PulsarConnectionDetails object for the given
	 * DockerComposeConnectionSource
	 */
	@Override
	protected PulsarConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PulsarDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link PulsarConnectionDetails} backed by a {@code pulsar} {@link RunningService}.
	 */
	static class PulsarDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements PulsarConnectionDetails {

		private final String brokerUrl;

		private final String adminUrl;

		/**
		 * Constructs a new PulsarDockerComposeConnectionDetails object with the provided
		 * RunningService.
		 * @param service the RunningService object representing the Pulsar service
		 */
		PulsarDockerComposeConnectionDetails(RunningService service) {
			super(service);
			ConnectionPorts ports = service.ports();
			this.brokerUrl = "pulsar://%s:%s".formatted(service.host(), ports.get(BROKER_PORT));
			this.adminUrl = "http://%s:%s".formatted(service.host(), ports.get(ADMIN_PORT));
		}

		/**
		 * Returns the broker URL for the Pulsar Docker Compose connection details.
		 * @return the broker URL
		 */
		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		/**
		 * Returns the admin URL for the Pulsar Docker Compose connection details.
		 * @return the admin URL
		 */
		@Override
		public String getAdminUrl() {
			return this.adminUrl;
		}

	}

}
