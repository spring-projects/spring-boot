/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.health;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for the
 * RabbitMQ messaging system.
 *
 * @author Christian Dupuis
 * @since 4.2.0
 */
public class RabbitHealthIndicator extends AbstractHealthIndicator {

	private final RabbitTemplate rabbitTemplate;

	public RabbitHealthIndicator(RabbitTemplate rabbitTemplate) {
		super("Rabbit health check failed");
		Assert.notNull(rabbitTemplate, "'rabbitTemplate' must not be null");
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.up();
		String version = getVersion();
		if (version != null) {
			builder.withDetail("version", version);
		}
	}

	private @Nullable String getVersion() {
		return this.rabbitTemplate.execute((channel) -> {
			Object version = channel.getConnection().getServerProperties().getOrDefault("version", "unknown");
			Assert.state(version != null, "'version' must not be null");
			return version.toString();
		});
	}

}
