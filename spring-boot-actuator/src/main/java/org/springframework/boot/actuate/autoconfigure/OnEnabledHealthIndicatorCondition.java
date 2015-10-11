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
 */
class OnEnabledHealthIndicatorCondition extends SpringBootCondition {

	private static final String ANNOTATION_CLASS = ConditionalOnEnabledHealthIndicator.class
			.getName();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ANNOTATION_CLASS));
		String endpointName = annotationAttributes.getString("value");
		ConditionOutcome outcome = getHealthIndicatorOutcome(context, endpointName);
		if (outcome != null) {
			return outcome;
		}
		return getDefaultIndicatorsOutcome(context);
	}

	private ConditionOutcome getHealthIndicatorOutcome(ConditionContext context,
			String endpointName) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "management.health." + endpointName + ".");
		if (resolver.containsProperty("enabled")) {
			boolean match = resolver.getProperty("enabled", Boolean.class, true);
			return new ConditionOutcome(match, "The health indicator " + endpointName
					+ " is " + (match ? "enabled" : "disabled"));
		}
		return null;
	}

	private ConditionOutcome getDefaultIndicatorsOutcome(ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "management.health.defaults.");
		boolean match = Boolean.valueOf(resolver.getProperty("enabled", "true"));
		return new ConditionOutcome(match, "All default health indicators are "
				+ (match ? "enabled" : "disabled") + " by default");
	}

}
