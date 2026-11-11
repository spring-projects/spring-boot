/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.PingHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class HealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class));

	@Test
	void runWhenNoOtherIndicatorsCreatesPingHealthIndicator() {
		this.contextRunner.run((context) -> assertThat(context).getBean(HealthIndicator.class)
			.isInstanceOf(PingHealthIndicator.class));
	}

	@Test
	void runWhenHasDefinedIndicatorCreatesPingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(PingHealthIndicator.class)
				.hasSingleBean(CustomHealthIndicator.class));
	}

	@Test
	void runWhenHasDefaultsDisabledDoesNotCreatePingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.withPropertyValues("management.health.defaults.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
	}

	@Test
	void runWhenHasDefaultsDisabledAndPingIndicatorEnabledCreatesPingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.withPropertyValues("management.health.defaults.enabled:false", "management.health.ping.enabled:true")
			.run((context) -> assertThat(context).hasSingleBean(PingHealthIndicator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnEnabledHealthIndicator("custom")
		HealthIndicator customHealthIndicator() {
			return new CustomHealthIndicator();
		}

	}

	static class CustomHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			return Health.down().build();
		}

	}

}
