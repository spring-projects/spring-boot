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

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link RabbitConnectionDetails}
 * for a {@code rabbitmq} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitStreamDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<RabbitStreamConnectionDetails> {

	private static final int RABBITMQ_STREAMS_PORT = 5552;

	protected RabbitStreamDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected @Nullable RabbitStreamConnectionDetails getDockerComposeConnectionDetails(
			DockerComposeConnectionSource source) {
		try {
			return new RabbitStreamDockerComposeConnectionDetails(source.getRunningService());
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * {@link RabbitStreamConnectionDetails} backed by a {@code rabbitmq}
	 * {@link RunningService}.
	 */
	static class RabbitStreamDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements RabbitStreamConnectionDetails {

		private final RabbitEnvironment environment;

		private final String host;

		private final int port;

		protected RabbitStreamDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new RabbitEnvironment(service.env());
			this.host = service.host();
			this.port = service.ports().get(RABBITMQ_STREAMS_PORT);
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
		public String getHost() {
			return this.host;
		}

		@Override
		public int getPort() {
			return this.port;
		}

	}

}
