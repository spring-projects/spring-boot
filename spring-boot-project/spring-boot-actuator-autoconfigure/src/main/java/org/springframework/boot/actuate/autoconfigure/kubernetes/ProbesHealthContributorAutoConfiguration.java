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

package org.springframework.boot.actuate.autoconfigure.kubernetes;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.kubernetes.ProbesHealthContributorAutoConfiguration.KubernetesOrPropertyCondition;
import org.springframework.boot.actuate.availability.LivenessProbeHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessProbeHealthIndicator;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistryCustomizer;
import org.springframework.boot.actuate.kubernetes.ProbesHealthEndpointGroupsRegistrar;
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
 * {@link LivenessProbeHealthIndicator} and {@link ReadinessProbeHealthIndicator}.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@Conditional(KubernetesOrPropertyCondition.class)
@AutoConfigureAfter(ApplicationAvailabilityAutoConfiguration.class)
public class ProbesHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("livenessProbe")
	@ConditionalOnMissingBean
	public LivenessProbeHealthIndicator livenessProbeHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new LivenessProbeHealthIndicator(applicationAvailability);
	}

	@Bean
	@ConditionalOnEnabledHealthIndicator("readinessProbe")
	@ConditionalOnMissingBean
	public ReadinessProbeHealthIndicator readinessProbeHealthIndicator(
			ApplicationAvailability applicationAvailability) {
		return new ReadinessProbeHealthIndicator(applicationAvailability);
	}

	@Bean
	public HealthEndpointGroupsRegistryCustomizer probesRegistryCustomizer() {
		return new ProbesHealthEndpointGroupsRegistrar();
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
