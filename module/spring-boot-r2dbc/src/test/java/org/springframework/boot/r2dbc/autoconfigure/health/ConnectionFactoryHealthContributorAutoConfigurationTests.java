/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.health.ConnectionFactoryHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConnectionFactoryHealthContributorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ConnectionFactoryHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ConnectionFactoryHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(ConnectionFactoryHealthIndicator.class));
	}

	@Test
	void runWithNoConnectionFactoryShouldNotCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withPropertyValues("management.health.r2dbc.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryHealthIndicator.class));
	}

}
