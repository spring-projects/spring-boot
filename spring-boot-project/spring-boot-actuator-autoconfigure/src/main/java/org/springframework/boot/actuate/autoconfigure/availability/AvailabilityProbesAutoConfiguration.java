/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for availability probes.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.3.0
 */
@AutoConfiguration(after = { AvailabilityHealthContributorAutoConfiguration.class,
		ApplicationAvailabilityAutoConfiguration.class })
@Conditional(AvailabilityProbesAutoConfiguration.ProbesCondition.class)
public class AvailabilityProbesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "livenessStateHealthIndicator")
	public LivenessStateHealthIndicator livenessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
		return new LivenessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	@ConditionalOnMissingBean(name = "readinessStateHealthIndicator")
	public ReadinessStateHealthIndicator readinessStateHealthIndicator(
			ApplicationAvailability applicationAvailability) {
		return new ReadinessStateHealthIndicator(applicationAvailability);
	}

	@Bean
	public AvailabilityProbesHealthEndpointGroupsPostProcessor availabilityProbesHealthEndpointGroupsPostProcessor(
			Environment environment) {
		return new AvailabilityProbesHealthEndpointGroupsPostProcessor(environment);
	}

	/**
	 * {@link SpringBootCondition} to enable or disable probes.
	 * <p>
	 * Probes are enabled if the dedicated configuration property is enabled or if the
	 * Kubernetes cloud environment is detected/enforced.
	 */
	static class ProbesCondition extends SpringBootCondition {

		private static final String ENABLED_PROPERTY = "management.endpoint.health.probes.enabled";

		private static final String DEPRECATED_ENABLED_PROPERTY = "management.health.probes.enabled";

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			ConditionMessage.Builder message = ConditionMessage.forCondition("Probes availability");
			ConditionOutcome outcome = onProperty(environment, message, ENABLED_PROPERTY);
			if (outcome != null) {
				return outcome;
			}
			outcome = onProperty(environment, message, DEPRECATED_ENABLED_PROPERTY);
			if (outcome != null) {
				return outcome;
			}
			if (CloudPlatform.getActive(environment) == CloudPlatform.KUBERNETES) {
				return ConditionOutcome.match(message.because("running on Kubernetes"));
			}
			return ConditionOutcome.noMatch(message.because("not running on a supported cloud platform"));
		}

		private ConditionOutcome onProperty(Environment environment, ConditionMessage.Builder message,
				String propertyName) {
			String enabled = environment.getProperty(propertyName);
			if (enabled != null) {
				boolean match = !"false".equalsIgnoreCase(enabled);
				return new ConditionOutcome(match, message.because("'" + propertyName + "' set to '" + enabled + "'"));
			}
			return null;
		}

	}

}
