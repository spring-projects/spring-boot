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

package org.springframework.boot.docker.compose.service.connection.activemq;

import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ActiveMQConnectionDetails} for an {@code activemq} service.
 *
 * @author Stephane Nicoll
 */
class ActiveMQDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ActiveMQConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	protected ActiveMQDockerComposeConnectionDetailsFactory() {
		super("symptoma/activemq");
	}

	@Override
	protected ActiveMQConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ActiveMQDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link RabbitConnectionDetails} backed by a {@code rabbitmq}
	 * {@link RunningService}.
	 */
	static class ActiveMQDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ActiveMQConnectionDetails {

		private final ActiveMQEnvironment environment;

		private final String brokerUrl;

		protected ActiveMQDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ActiveMQEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
		}

		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		@Override
		public String getUser() {
			return this.environment.getUser();
		}

		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

	}

}
