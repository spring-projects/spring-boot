/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitAutoConfiguration;
import org.springframework.boot.amqp.rabbitmq.health.AmqpRabbitHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AmqpRabbitHealthContributorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class AmqpRabbitHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AmqpRabbitAutoConfiguration.class,
				AmqpRabbitHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(AmqpRabbitHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.rabbit.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(AmqpRabbitHealthIndicator.class));
	}

}
