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

package org.springframework.boot.amqp.rabbitmq.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link AmqpRabbitConnectionDetails} for a {@code rabbitmq} service.
 *
 * @author Eddú Meléndez
 */
class AmqpRabbitMqDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<AmqpRabbitConnectionDetails> {

	private static final int RABBITMQ_PORT = 5672;

	private static final int RABBITMQ_TLS_PORT = 5671;

	protected AmqpRabbitMqDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected @Nullable AmqpRabbitConnectionDetails getDockerComposeConnectionDetails(
			DockerComposeConnectionSource source) {
		try {
			return new AmqpRabbitMqDockerComposeRabbitConnectionDetails(source.getRunningService());
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * {@link AmqpRabbitConnectionDetails} backed by a {@code rabbitmq}
	 * {@link RunningService}.
	 */
	static class AmqpRabbitMqDockerComposeRabbitConnectionDetails extends DockerComposeConnectionDetails
			implements AmqpRabbitConnectionDetails {

		private final Address address;

		private final RabbitEnvironment environment;

		private final @Nullable SslBundle sslBundle;

		protected AmqpRabbitMqDockerComposeRabbitConnectionDetails(RunningService service) {
			super(service);
			this.sslBundle = getSslBundle(service);
			int containerPort = (this.sslBundle != null) ? RABBITMQ_TLS_PORT : RABBITMQ_PORT;
			this.address = new Address(service.host(), service.ports().get(containerPort));
			this.environment = new RabbitEnvironment(service.env());
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

		@Override
		public String getVirtualHost() {
			return "/";
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

	}

}
