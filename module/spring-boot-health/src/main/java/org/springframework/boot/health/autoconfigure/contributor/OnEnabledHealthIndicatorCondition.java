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

package org.springframework.boot.health.autoconfigure.contributor;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a health indicator is enabled.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OnEnabledHealthIndicatorCondition extends SpringBootCondition {

	private static final String DEFAULTS_PROPERTY_NAME = "management.health.defaults.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MergedAnnotation<?> annotation = metadata.getAnnotations().get(ConditionalOnEnabledHealthIndicator.class);
		String name = annotation.getString(MergedAnnotation.VALUE);
		Environment environment = context.getEnvironment();
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnEnabledHealthIndicator.class);
		String propertyName = "management.health." + name + ".enabled";
		if (environment.containsProperty(propertyName)) {
			boolean match = environment.getProperty(propertyName, Boolean.class, true);
			return new ConditionOutcome(match, message.because(propertyName + " is " + match));
		}
		boolean match = Boolean.parseBoolean(context.getEnvironment().getProperty(DEFAULTS_PROPERTY_NAME, "true"));
		return new ConditionOutcome(match, message.because(DEFAULTS_PROPERTY_NAME + " is considered " + match));
	}

}
