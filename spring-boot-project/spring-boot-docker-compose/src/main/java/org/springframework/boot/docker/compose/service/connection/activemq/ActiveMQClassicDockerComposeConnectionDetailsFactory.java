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

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ActiveMQConnectionDetails} for an {@code activemq} service.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ActiveMQClassicDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ActiveMQConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	/**
	 * Constructs a new instance of ActiveMQClassicDockerComposeConnectionDetailsFactory.
	 *
	 * This constructor sets the Docker image name to "apache/activemq-classic".
	 */
	protected ActiveMQClassicDockerComposeConnectionDetailsFactory() {
		super("apache/activemq-classic");
	}

	/**
	 * Returns the ActiveMQConnectionDetails for the given DockerComposeConnectionSource.
	 * @param source the DockerComposeConnectionSource containing the running service
	 * information
	 * @return the ActiveMQConnectionDetails for the given DockerComposeConnectionSource
	 */
	@Override
	protected ActiveMQConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ActiveMQDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ActiveMQConnectionDetails} backed by a {@code activemq}
	 * {@link RunningService}.
	 */
	static class ActiveMQDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ActiveMQConnectionDetails {

		private final ActiveMQClassicEnvironment environment;

		private final String brokerUrl;

		/**
		 * Constructs a new ActiveMQDockerComposeConnectionDetails object with the
		 * provided RunningService.
		 * @param service the RunningService object representing the ActiveMQ service
		 */
		protected ActiveMQDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ActiveMQClassicEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
		}

		/**
		 * Returns the broker URL of the ActiveMQDockerComposeConnectionDetails.
		 * @return the broker URL of the ActiveMQDockerComposeConnectionDetails
		 */
		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		/**
		 * Returns the user associated with the current environment.
		 * @return the user associated with the current environment
		 */
		@Override
		public String getUser() {
			return this.environment.getUser();
		}

		/**
		 * Returns the password for the ActiveMQ connection.
		 * @return the password for the ActiveMQ connection
		 */
		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

	}

}
