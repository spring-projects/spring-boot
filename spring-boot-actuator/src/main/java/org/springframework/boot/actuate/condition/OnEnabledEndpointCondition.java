/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.condition;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks whether or not an endpoint is enabled.
 *
 * @author Andy Wilkinson
 */
class OnEnabledEndpointCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(ConditionalOnEnabledEndpoint.class.getName()));
		String endpointName = annotationAttributes.getString("value");
		boolean enabledByDefault = annotationAttributes.getBoolean("enabledByDefault");
		ConditionOutcome outcome = determineEndpointOutcome(endpointName,
				enabledByDefault, context);
		if (outcome != null) {
			return outcome;
		}
		return determineAllEndpointsOutcome(context);
	}

	private ConditionOutcome determineEndpointOutcome(String endpointName,
			boolean enabledByDefault, ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "endpoints." + endpointName + ".");
		if (resolver.containsProperty("enabled") || !enabledByDefault) {
			boolean match = resolver.getProperty("enabled", Boolean.class,
					enabledByDefault);
			ConditionMessage message = ConditionMessage
					.forCondition(ConditionalOnEnabledEndpoint.class,
							"(" + endpointName + ")")
					.because(match ? "enabled" : "disabled");
			return new ConditionOutcome(match, message);
		}
		return null;
	}

	private ConditionOutcome determineAllEndpointsOutcome(ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "endpoints.");
		boolean match = Boolean.valueOf(resolver.getProperty("enabled", "true"));
		ConditionMessage message = ConditionMessage
				.forCondition(ConditionalOnEnabledEndpoint.class)
				.because("All endpoints are " + (match ? "enabled" : "disabled")
						+ " by default");
		return new ConditionOutcome(match, message);
	}

}
