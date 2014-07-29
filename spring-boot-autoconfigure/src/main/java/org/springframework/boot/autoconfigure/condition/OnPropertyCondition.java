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
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
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

		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(ConditionalOnProperty.class.getName()));

		String prefix = annotationAttributes.getString("prefix").trim();
		if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		String havingValue = annotationAttributes.getString("havingValue");
		String[] names = getNames(annotationAttributes);
		boolean relaxedNames = annotationAttributes.getBoolean("relaxedNames");
		boolean matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");

		PropertyResolver resolver = context.getEnvironment();
		if (relaxedNames) {
			resolver = new RelaxedPropertyResolver(resolver, prefix);
		}

		List<String> missingProperties = new ArrayList<String>();
		List<String> nonMatchingProperties = new ArrayList<String>();
		for (String name : names) {
			String key = (relaxedNames ? name : prefix + name);
			if (resolver.containsProperty(key)) {
				if (!isMatch(resolver.getProperty(key), havingValue)) {
					nonMatchingProperties.add(name);
				}
			}
			else {
				if (!matchIfMissing) {
					missingProperties.add(name);
				}
			}
		}

		if (missingProperties.isEmpty() && nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.match();
		}

		StringBuilder message = new StringBuilder("@ConditionalOnProperty ");
		if (!missingProperties.isEmpty()) {
			message.append("missing required properties "
					+ expandNames(prefix, missingProperties) + " ");
		}
		if (!nonMatchingProperties.isEmpty()) {
			String expected = havingValue == null ? "!false" : havingValue;
			message.append("expected '").append(expected).append("' for properties ")
					.append(expandNames(prefix, nonMatchingProperties));
		}

		return ConditionOutcome.noMatch(message.toString());
	}

	private String[] getNames(Map<String, Object> annotationAttributes) {
		String[] value = (String[]) annotationAttributes.get("value");
		String[] name = (String[]) annotationAttributes.get("name");
		Assert.state(value.length > 0 || name.length > 0,
				"The name or value attribute of @ConditionalOnProperty must be specified");
		Assert.state(value.length == 0 || name.length == 0,
				"The name and value attributes of @ConditionalOnProperty are exclusive");
		return (value.length > 0 ? value : name);
	}

	private boolean isMatch(String value, String requiredValue) {
		if (StringUtils.hasLength(requiredValue)) {
			return requiredValue.equalsIgnoreCase(value);
		}
		return !"false".equalsIgnoreCase(value);
	}

	private String expandNames(String prefix, List<String> names) {
		StringBuffer expanded = new StringBuffer();
		for (String name : names) {
			expanded.append(expanded.length() == 0 ? "" : ", ");
			expanded.append(prefix);
			expanded.append(name);
		}
		return expanded.toString();
	}

}
