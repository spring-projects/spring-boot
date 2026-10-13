/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure.health;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.jms.health.JmsHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class JmsHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmsHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class))
		.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(JmsHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.jms.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(JmsHealthIndicator.class));
	}

}
