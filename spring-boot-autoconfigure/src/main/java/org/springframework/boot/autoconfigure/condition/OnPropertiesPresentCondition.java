/*
 * Copyright 2012-2014 the original author or authors.
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

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @see ConditionalOnPropertiesPresent
 * @since 1.1.0
 */
public class OnPropertiesPresentCondition extends SpringBootCondition {

	@Override public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		String[] properties = (String[]) metadata.getAnnotationAttributes(
				ConditionalOnPropertiesPresent.class.getName()).get("value");

		ConditionOutcome conditionOutcome = ConditionOutcome.match();

		List<String> missingProperties = new ArrayList<String>();

		for (String property : properties) {
			if (!context.getEnvironment().containsProperty(property)) {
				missingProperties.add(property);
			}
		}

		if (!missingProperties.isEmpty()) {
			conditionOutcome = ConditionOutcome.noMatch(
					"Required properties: " + StringUtils.arrayToCommaDelimitedString(
							missingProperties.toArray())
							+ " not found"
			);
		}

		return conditionOutcome;
	}
}
