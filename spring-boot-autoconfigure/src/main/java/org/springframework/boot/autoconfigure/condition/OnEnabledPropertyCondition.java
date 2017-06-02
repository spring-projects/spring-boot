/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The implementation for checking {@link ConditionalOnEnabledProperty}
 *
 * @author 20 Apr 2017 idosu(Ido Sorozon)
 * @see ConditionalOnEnabledProperty
 */
public class OnEnabledPropertyCondition extends SpringBootCondition {
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		// Checks that the bean is ConfigurationProperties
		if (!metadata.isAnnotated(ConfigurationProperties.class.getName())) {
			return ConditionOutcome.noMatch(ConditionMessage
				.forCondition(ConditionalOnEnabledProperty.class)
				.didNotFind("annotation")
				.items(ConfigurationProperties.class)
			);
		}

		// Gets the prefix
		AnnotationAttributes annotation = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(ConfigurationProperties.class.getName()));
		String prefix = annotation.getString("prefix");

		// TODO(idosu): I do not know if I need to check this, but if so maybe I should change this to Assert?
		// Validates that the prefix hes been set
		if (prefix.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage
				.forCondition(ConditionalOnEnabledProperty.class)
				.because("could not find property 'prefix' or 'value' inside @ConfigurationProperties")
			);
		}


		// Check if the property is true
		String property = prefix + "." + "enabled";
		if (context.getEnvironment() == null || !"true".equalsIgnoreCase(context.getEnvironment().getProperty(property))) {
			return ConditionOutcome.noMatch(ConditionMessage
				.forCondition(ConditionalOnEnabledProperty.class)
				.because("the property value is not 'true'")
			);
		}

		// Matches
		return ConditionOutcome.match(ConditionMessage
			.forCondition(ConditionalOnEnabledProperty.class)
			.because("matched")
		);
	}
}
