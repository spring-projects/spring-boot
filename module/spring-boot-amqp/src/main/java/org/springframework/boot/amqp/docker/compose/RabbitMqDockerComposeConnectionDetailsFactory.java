/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link AmqpConnectionDetails}
 * for a {@code rabbitmq} service.
 *
 * @author Stephane Nicoll
 */
class RabbitMqDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<AmqpConnectionDetails> {

	private static final int RABBITMQ_PORT = 5672;

	protected RabbitMqDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected @Nullable AmqpConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		try {
			return new RabbitMqDockerComposeConnectionDetails(source.getRunningService());
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * {@link AmqpConnectionDetails} backed by a {@code rabbitmq} {@link RunningService}.
	 */
	static class RabbitMqDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements AmqpConnectionDetails {

		private final RabbitMqEnvironment environment;

		private final Address address;

		protected RabbitMqDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new RabbitMqEnvironment(service.env());
			this.address = new Address(service.host(), service.ports().get(RABBITMQ_PORT));
		}

		@Override
		public Address getAddress() {
			return this.address;
		}

		@Override
		public @Nullable String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

	}

}
