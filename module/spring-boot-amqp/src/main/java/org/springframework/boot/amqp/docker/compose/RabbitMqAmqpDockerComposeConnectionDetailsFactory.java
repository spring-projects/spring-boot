/*
 * Copyright 2012-present the original author or authors.
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
		String username = extract(env, "RABBITMQ_DEFAULT_USER", "RABBITMQ_USERNAME");
		String password = extract(env, "RABBITMQ_DEFAULT_PASS", "RABBITMQ_PASSWORD");
		return new AmqpEnvironment(username, password);
	}

	private String extract(Map<String, @Nullable String> env, String key, String fallbackKey) {
		String value = env.get(key);
		value = (value != null) ? value : env.get(fallbackKey);
		return (value != null) ? value : "guest";
	}

}
