/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.health.application.LivenessStateHealthIndicator;
import org.springframework.boot.health.application.ReadinessStateHealthIndicator;
import org.springframework.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AvailabilityProbesAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class AvailabilityProbesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ApplicationAvailabilityAutoConfiguration.class,
				AvailabilityHealthContributorAutoConfiguration.class, AvailabilityProbesAutoConfiguration.class));

	@Test
	void probesWhenDefaultAddsBeans() {
		this.contextRunner.run(this::hasProbesBeans);
	}

	@Test
	void probesWhenPropertyEnabledAddsBeans() {
		this.contextRunner.withPropertyValues("management.endpoint.health.probes.enabled=true")
			.run(this::hasProbesBeans);
	}

	@Test
	void probesWhenNoActuatorDependencyDoesNotAddBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Endpoint.class.getName()))
			.run(this::doesNotHaveProbeBeans);
	}

	@Test
	void probesWhenPropertyDisabledAddsNotBeans() {
		this.contextRunner.withPropertyValues("management.endpoint.health.probes.enabled=false")
			.run(this::doesNotHaveProbeBeans);
	}

	private void hasProbesBeans(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ApplicationAvailability.class)
			.hasSingleBean(LivenessStateHealthIndicator.class)
			.hasBean("livenessStateHealthIndicator")
			.hasSingleBean(ReadinessStateHealthIndicator.class)
			.hasBean("readinessStateHealthIndicator")
			.hasSingleBean(AvailabilityProbesHealthEndpointGroupsPostProcessor.class);
	}

	private void doesNotHaveProbeBeans(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ApplicationAvailability.class)
			.doesNotHaveBean(LivenessStateHealthIndicator.class)
			.doesNotHaveBean(ReadinessStateHealthIndicator.class)
			.doesNotHaveBean(AvailabilityProbesHealthEndpointGroupsPostProcessor.class);
	}

}
