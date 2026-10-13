/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.boot.data.redis.health.DataRedisReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisReactiveHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class DataRedisReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class,
				DataRedisReactiveHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DataRedisReactiveHealthIndicator.class)
			.hasBean("redisHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataRedisHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(DataRedisReactiveHealthIndicator.class)
				.hasBean("redisHealthContributor")
				.doesNotHaveBean(DataRedisHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.redis.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(DataRedisReactiveHealthIndicator.class)
				.doesNotHaveBean("redisHealthContributor"));
	}

}
