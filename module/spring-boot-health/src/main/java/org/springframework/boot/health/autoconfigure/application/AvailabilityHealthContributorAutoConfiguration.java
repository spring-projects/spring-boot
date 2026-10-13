/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.application;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.health.application.AvailabilityStateHealthIndicator;
import org.springframework.boot.health.application.LivenessStateHealthIndicator;
import org.springframework.boot.health.application.ReadinessStateHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link AvailabilityStateHealthIndicator}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(after = ApplicationAvailabilityAutoConfiguration.class)
@ConditionalOnClass(Health.class)
public final class AvailabilityHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "livenessStateHealthIndicator")
	@ConditionalOnBooleanProperty("management.health.livenessstate.enabled")
	LivenessStateHealthIndicator livenessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new LivenessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	@ConditionalOnMissingBean(name = "readinessStateHealthIndicator")
	@ConditionalOnBooleanProperty("management.health.readinessstate.enabled")
	ReadinessStateHealthIndicator readinessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new ReadinessStateHealthIndicator(applicationAvailability);
	}

}
