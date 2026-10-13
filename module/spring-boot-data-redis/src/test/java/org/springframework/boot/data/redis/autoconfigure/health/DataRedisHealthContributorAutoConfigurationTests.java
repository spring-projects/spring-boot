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
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 */
@ClassPathExclusions({ "reactor-core*.jar", "lettuce-core*.jar" })
class DataRedisHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class,
				DataRedisHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DataRedisHealthIndicator.class)
			.doesNotHaveBean(DataRedisReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.redis.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(DataRedisHealthIndicator.class)
				.doesNotHaveBean(DataRedisReactiveHealthIndicator.class));
	}

}
