/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityProbesAutoConfiguration.KubernetesOrPropertyCondition;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link LivenessStateHealthIndicator}, {@link ReadinessStateHealthIndicator} and
 * {@link HealthEndpointGroupsRegistryCustomizer}.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@Conditional(KubernetesOrPropertyCondition.class)
@AutoConfigureAfter(ApplicationAvailabilityAutoConfiguration.class)
public class AvailabilityProbesAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("livenessState")
	@ConditionalOnMissingBean
	public LivenessStateHealthIndicator livenessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new LivenessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	@ConditionalOnEnabledHealthIndicator("readinessState")
	@ConditionalOnMissingBean
	public ReadinessStateHealthIndicator readinessStateHealthIndicator(
			ApplicationAvailability applicationAvailability) {
		return new ReadinessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	public HealthEndpointGroupsRegistryCustomizer probesRegistryCustomizer() {
		return new AvailabilityProbesHealthEndpointGroupsRegistrar();
	}

	static class KubernetesOrPropertyCondition extends AnyNestedCondition {

		KubernetesOrPropertyCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
		static class Kubernetes {

		}

		@ConditionalOnProperty(prefix = "management.health.probes", name = "enabled")
		static class ProbesIndicatorsEnabled {

		}

	}

}
