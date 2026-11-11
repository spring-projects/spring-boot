/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.rabbitmq.health.RabbitHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class RabbitHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class,
				RabbitHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RabbitHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.rabbit.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(RabbitHealthIndicator.class));
	}

}
