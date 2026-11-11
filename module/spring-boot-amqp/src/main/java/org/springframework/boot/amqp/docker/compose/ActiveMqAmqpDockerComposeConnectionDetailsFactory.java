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
 * for an {@code activemq} service.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ActiveMqAmqpDockerComposeConnectionDetailsFactory extends AbstractAmqpDockerComposeConnectionDetailsFactory {

	ActiveMqAmqpDockerComposeConnectionDetailsFactory() {
		super(new String[] { "apache/activemq-classic", "apache/activemq" });
	}

	@Override
	protected AmqpEnvironment getAmqpEnvironment(RunningService service) {
		Map<String, @Nullable String> env = service.env();
		return new AmqpEnvironment(env.get("ACTIVEMQ_CONNECTION_USER"), env.get("ACTIVEMQ_CONNECTION_PASSWORD"));
	}

}
