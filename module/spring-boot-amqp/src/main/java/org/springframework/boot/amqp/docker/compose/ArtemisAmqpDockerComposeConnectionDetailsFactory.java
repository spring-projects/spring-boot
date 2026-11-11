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
 * for a {@code artemis} service.
 *
 * @author Stephane Nicoll
 */
class ArtemisAmqpDockerComposeConnectionDetailsFactory extends AbstractAmqpDockerComposeConnectionDetailsFactory {

	ArtemisAmqpDockerComposeConnectionDetailsFactory() {
		super(new String[] { "apache/activemq-artemis", "apache/artemis" });
	}

	@Override
	protected AmqpEnvironment getAmqpEnvironment(RunningService service) {
		Map<String, @Nullable String> env = service.env();
		return new AmqpEnvironment(env.get("ARTEMIS_USER"), env.get("ARTEMIS_PASSWORD"));
	}

}
