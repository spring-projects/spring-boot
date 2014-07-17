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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see ConditionalOnProperty
 * @since 1.1.0
 */
class OnPropertyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(
				ConditionalOnProperty.class.getName());

		String prefix = getPrefix(annotationAttributes);
		String expectedValue = getExpectedValue(annotationAttributes);
		String[] names = (String[]) annotationAttributes.get("value");
		boolean relaxedNames = (Boolean) annotationAttributes.get("relaxedNames");
		boolean matchDefault = (Boolean) annotationAttributes.get("defaultMatch");

		List<String> missingProperties = new ArrayList<String>();
		List<String> nonMatchingProperties = new ArrayList<String>();

		PropertyResolver resolver = context.getEnvironment();
		if (relaxedNames) {
			resolver = new RelaxedPropertyResolver(resolver, prefix);
			prefix = "";
		}

		for (String name : names) {
			name = prefix + name;
			boolean hasProperty = resolver.containsProperty(name);
			if (!hasProperty) {  // property not set
				if (!matchDefault) { // property is mandatory
					missingProperties.add(name);
				}
			}
			else {
				String actualValue = resolver.getProperty(name);
				if (expectedValue == null) {
					if ("false".equalsIgnoreCase(actualValue)) {
						nonMatchingProperties.add(name);
					}
				}
				else if (!expectedValue.equalsIgnoreCase(actualValue)) {
					nonMatchingProperties.add(name);
				}
			}
		}

		if (missingProperties.isEmpty() && nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.match();
		}

		StringBuilder sb = new StringBuilder("@ConditionalOnProperty ");
		if (!matchDefault && !missingProperties.isEmpty()) {
			sb.append("missing required properties ")
					.append(StringUtils.arrayToCommaDelimitedString(missingProperties.toArray()))
					.append(" ");
		}
		if (!nonMatchingProperties.isEmpty()) {
			String expected = expectedValue == null ? "!false" : expectedValue;
			sb.append("expected '").append(expected).append("' for properties: ")
					.append(StringUtils.arrayToCommaDelimitedString(nonMatchingProperties.toArray()));
		}

		return ConditionOutcome.noMatch(sb.toString());
	}

	/**
	 * Return the prefix to use or an empty String if it's not set.
	 * <p>Add a dot at the end if it is not present already.
	 */
	private static String getPrefix(Map<String, Object> annotationAttributes) {
		String prefix = ((String) annotationAttributes.get("prefix")).trim();
		if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		return prefix;
	}

	/**
	 * Return the expected value to match against or {@code null} if no
	 * match value is set.
	 */
	private static String getExpectedValue(Map<String, Object> annotationAttributes) {
		String match = (String) annotationAttributes.get("match");
		if (StringUtils.hasText(match)) {
			return match;
		}
		return null;
	}
}
