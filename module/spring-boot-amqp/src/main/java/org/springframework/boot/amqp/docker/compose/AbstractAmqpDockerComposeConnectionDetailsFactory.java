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
 * Base {@link DockerComposeConnectionDetailsFactory} to create
 * {@link AmqpConnectionDetails} from an AMQP 1.0-compliant service.
 *
 * @author Stephane Nicoll
 */
abstract class AbstractAmqpDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<AmqpConnectionDetails> {

	protected AbstractAmqpDockerComposeConnectionDetailsFactory(String connectionName) {
		super(connectionName);
	}

	protected AbstractAmqpDockerComposeConnectionDetailsFactory(String[] connectionNames,
			String... requiredClassNames) {
		super(connectionNames, requiredClassNames);
	}

	@Override
	protected final @Nullable AmqpConnectionDetails getDockerComposeConnectionDetails(
			DockerComposeConnectionSource source) {
		RunningService runningService = source.getRunningService();
		AmqpEnvironment environment = getAmqpEnvironment(runningService);
		return new AmqpDockerComposeConnectionDetails(runningService, environment);
	}

	protected abstract AmqpEnvironment getAmqpEnvironment(RunningService service);

	static class AmqpDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements AmqpConnectionDetails {

		private static final int AMQP_PORT = 5672;

		private final Address address;

		private final @Nullable String username;

		private final @Nullable String password;

		AmqpDockerComposeConnectionDetails(RunningService runningService, AmqpEnvironment environment) {
			super(runningService);
			this.address = new Address(runningService.host(), runningService.ports().get(AMQP_PORT));
			this.username = environment.getUsername();
			this.password = environment.getPassword();
		}

		@Override
		public Address getAddress() {
			return this.address;
		}

		@Override
		public @Nullable String getUsername() {
			return this.username;
		}

		@Override
		public @Nullable String getPassword() {
			return this.password;
		}

	}

}
