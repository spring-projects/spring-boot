/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.application;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.health.application.LivenessStateHealthIndicator;
import org.springframework.boot.health.application.ReadinessStateHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AvailabilityHealthContributorAutoConfiguration}
 *
 * @author Brian Clozel
 */
class AvailabilityHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ApplicationAvailabilityAutoConfiguration.class,
				AvailabilityHealthContributorAutoConfiguration.class));

	@Test
	void probesWhenNotKubernetesAddsNoBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
			.doesNotHaveBean(LivenessStateHealthIndicator.class)
			.doesNotHaveBean(ReadinessStateHealthIndicator.class));
	}

	@Test
	void livenessIndicatorWhenPropertyEnabledAddsBeans() {
		this.contextRunner.withPropertyValues("management.health.livenessState.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
				.hasSingleBean(LivenessStateHealthIndicator.class)
				.doesNotHaveBean(ReadinessStateHealthIndicator.class));
	}

	@Test
	void readinessIndicatorWhenPropertyEnabledAddsBeans() {
		this.contextRunner.withPropertyValues("management.health.readinessState.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
				.hasSingleBean(ReadinessStateHealthIndicator.class)
				.doesNotHaveBean(LivenessStateHealthIndicator.class));
	}

}
