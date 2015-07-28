/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a health indicator is enabled.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class OnEnabledHealthIndicatorCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(ConditionalOnEnablednHealthIndicator.class.getName()));

		String endpointName = annotationAttributes.getString("value");
		ConditionOutcome outcome = determineHealthIndicatorOutcome(endpointName, context);
		if (outcome != null) {
			return outcome;
		}
		return determineDefaultIndicatorsOutcome(context);
	}

	private ConditionOutcome determineHealthIndicatorOutcome(String endpointName,
			ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "management.health." + endpointName + ".");
		if (resolver.containsProperty("enabled")) {
			boolean match = resolver.getProperty("enabled", Boolean.class,
					true);
			return new ConditionOutcome(match, "The health indicator " + endpointName +
					" is " + (match ? "enabled" : "disabled"));
		}
		return null;
	}

	private ConditionOutcome determineDefaultIndicatorsOutcome(ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "management.health.defaults.");
		boolean match = Boolean.valueOf(resolver.getProperty("enabled", "true"));
		return new ConditionOutcome(match, "All default health indicators are "
				+ (match ? "enabled" : "disabled") + " by default");
	}

}
