/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.activemq;

import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConnectionDetails;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisMode;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ArtemisConnectionDetails} for an {@code artemis} service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ArtemisDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ArtemisConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	/**
     * Constructs a new instance of ArtemisDockerComposeConnectionDetailsFactory with the specified Docker image name.
     * 
     * @param dockerImageName the name of the Docker image to use for the Artemis broker
     */
    protected ArtemisDockerComposeConnectionDetailsFactory() {
		super("apache/activemq-artemis");
	}

	/**
     * Retrieves the connection details for a Docker Compose service.
     * 
     * @param source the source of the Docker Compose connection
     * @return the connection details for the Docker Compose service
     */
    @Override
	protected ArtemisConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ArtemisDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ArtemisConnectionDetails} backed by a {@code artemis}
	 * {@link RunningService}.
	 */
	static class ArtemisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ArtemisConnectionDetails {

		private final ArtemisEnvironment environment;

		private final String brokerUrl;

		/**
         * Constructs a new ArtemisDockerComposeConnectionDetails object with the provided RunningService.
         * 
         * @param service the RunningService object representing the running service
         */
        protected ArtemisDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ArtemisEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
		}

		/**
         * Returns the mode of the Artemis Docker Compose connection details.
         * 
         * @return the mode of the connection details, which is {@link ArtemisMode#NATIVE}
         */
        @Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		/**
         * Returns the broker URL for the Artemis Docker Compose connection details.
         *
         * @return the broker URL
         */
        @Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		/**
         * Returns the user associated with the current environment.
         * 
         * @return the user associated with the current environment
         */
        @Override
		public String getUser() {
			return this.environment.getUser();
		}

		/**
         * Returns the password for the connection details.
         * 
         * @return the password for the connection details
         */
        @Override
		public String getPassword() {
			return this.environment.getPassword();
		}

	}

}
