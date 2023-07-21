/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.availability;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.availability.ApplicationAvailability;
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
