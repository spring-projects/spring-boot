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

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link AmqpConnectionDetails}
 * for a {@code rabbitmq} service.
 *
 * @author Stephane Nicoll
 */
class RabbitMqAmqpDockerComposeConnectionDetailsFactory extends AbstractAmqpDockerComposeConnectionDetailsFactory {

	RabbitMqAmqpDockerComposeConnectionDetailsFactory() {
		super("rabbitmq");
	}

	@Override
	protected AmqpEnvironment getAmqpEnvironment(RunningService service) {
		Map<String, @Nullable String> env = service.env();
		String username = env.getOrDefault("RABBITMQ_DEFAULT_USER", env.getOrDefault("RABBITMQ_USERNAME", "guest"));
		String password = env.getOrDefault("RABBITMQ_DEFAULT_PASS", env.getOrDefault("RABBITMQ_PASSWORD", "guest"));
		return new AmqpEnvironment(username, password);
	}

}
