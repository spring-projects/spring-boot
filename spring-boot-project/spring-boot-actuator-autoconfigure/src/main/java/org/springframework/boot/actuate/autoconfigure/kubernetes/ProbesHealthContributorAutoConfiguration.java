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
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistryCustomizer;
import org.springframework.boot.actuate.kubernetes.LivenessProbeHealthIndicator;
import org.springframework.boot.actuate.kubernetes.ProbesHealthEndpointGroupsRegistrar;
import org.springframework.boot.actuate.kubernetes.ReadinessProbeHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.kubernetes.ApplicationStateAutoConfiguration;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.kubernetes.ApplicationStateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link LivenessProbeHealthIndicator} and {@link ReadinessProbeHealthIndicator}.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
@AutoConfigureAfter(ApplicationStateAutoConfiguration.class)
public class ProbesHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("livenessProbe")
	public LivenessProbeHealthIndicator livenessProbeHealthIndicator(
			ApplicationStateProvider applicationStateProvider) {
		return new LivenessProbeHealthIndicator(applicationStateProvider);
	}

	@Bean
	@ConditionalOnEnabledHealthIndicator("readinessProbe")
	public ReadinessProbeHealthIndicator readinessProbeHealthIndicator(
			ApplicationStateProvider applicationStateProvider) {
		return new ReadinessProbeHealthIndicator(applicationStateProvider);
	}

	@Bean
	public HealthEndpointGroupsRegistryCustomizer probesRegistryCustomizer() {
		return new ProbesHealthEndpointGroupsRegistrar();
	}

}
