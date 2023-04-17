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

package org.springframework.boot.docker.compose.service.connection.rabbit;

import java.util.List;

import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
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
 */
class RabbitDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<RabbitConnectionDetails> {

	private static final int RABBITMQ_PORT = 5672;

	protected RabbitDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected RabbitConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new RabbitDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link RabbitConnectionDetails} backed by a {@code rabbitmq}
	 * {@link RunningService}.
	 */
	static class RabbitDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements RabbitConnectionDetails {

		private final RabbitEnvironment environment;

		private final List<Address> addresses;

		protected RabbitDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new RabbitEnvironment(service.env());
			this.addresses = List.of(new Address(service.host(), service.ports().get(RABBITMQ_PORT)));
		}

		@Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public String getVirtualHost() {
			return "/";
		}

		@Override
		public List<Address> getAddresses() {
			return this.addresses;
		}

	}

}
