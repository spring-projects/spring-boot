/*
 * Copyright 2012-2016 the original author or authors.
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
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Base endpoint element condition. An element can be disabled globally via the
 * {@code defaults} name or individually via the name of the element.
 *
 * @author Stephane Nicoll
 */
abstract class OnEnabledEndpointElementCondition extends SpringBootCondition {

	private final String prefix;

	private final Class<?> annotationType;

	OnEnabledEndpointElementCondition(String prefix, Class<?> annotationType) {
		this.prefix = prefix;
		this.annotationType = annotationType;
	}

	protected String getEndpointElementOutcomeMessage(String name, boolean match) {
		return "The endpoint element " + name + " is " + (match ? "enabled" : "disabled");
	}

	protected String getDefaultEndpointElementOutcomeMessage(boolean match) {
		return "All default endpoint elements are " + (match ? "enabled" : "disabled")
				+ " by default";
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(this.annotationType.getName()));
		String endpointName = annotationAttributes.getString("value");
		ConditionOutcome outcome = getEndpointOutcome(context, endpointName);
		if (outcome != null) {
			return outcome;
		}
		return getDefaultEndpointsOutcome(context);
	}

	protected ConditionOutcome getEndpointOutcome(ConditionContext context,
			String endpointName) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), this.prefix + endpointName + ".");
		if (resolver.containsProperty("enabled")) {
			boolean match = resolver.getProperty("enabled", Boolean.class, true);
			return new ConditionOutcome(match,
					getEndpointElementOutcomeMessage(endpointName, match));
		}
		return null;
	}

	protected ConditionOutcome getDefaultEndpointsOutcome(ConditionContext context) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), this.prefix + "defaults.");
		boolean match = Boolean.valueOf(resolver.getProperty("enabled", "true"));
		return new ConditionOutcome(match,
				getDefaultEndpointElementOutcomeMessage(match));
	}

}
