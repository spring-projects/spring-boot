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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a metrics exporter is enabled.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class OnMetricsExportEnabledCondition extends SpringBootCondition {

	private static final String PROPERTY_TEMPLATE = "management.%s.metrics.export.enabled";

	private static final String DEFAULT_PROPERTY_NAME = "management.defaults.metrics.export.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ConditionalOnEnabledMetricsExport.class.getName()));
		String endpointName = annotationAttributes.getString("value");
		ConditionOutcome outcome = getProductOutcome(context, endpointName);
		if (outcome != null) {
			return outcome;
		}
		return getDefaultOutcome(context);
	}

	private ConditionOutcome getProductOutcome(ConditionContext context, String productName) {
		Environment environment = context.getEnvironment();
		String enabledProperty = PROPERTY_TEMPLATE.formatted(productName);
		if (environment.containsProperty(enabledProperty)) {
			boolean match = environment.getProperty(enabledProperty, Boolean.class, true);
			return new ConditionOutcome(match, ConditionMessage.forCondition(ConditionalOnEnabledMetricsExport.class)
					.because(enabledProperty + " is " + match));
		}
		return null;
	}

	/**
	 * Return the default outcome that should be used if property is not set. By default
	 * this method will use the {@link #DEFAULT_PROPERTY_NAME} property, matching if it is
	 * {@code true} or if it is not configured.
	 * @param context the condition context
	 * @return the default outcome
	 */
	private ConditionOutcome getDefaultOutcome(ConditionContext context) {
		boolean match = Boolean.parseBoolean(context.getEnvironment().getProperty(DEFAULT_PROPERTY_NAME, "true"));
		return new ConditionOutcome(match, ConditionMessage.forCondition(ConditionalOnEnabledMetricsExport.class)
				.because(DEFAULT_PROPERTY_NAME + " is considered " + match));
	}

}
