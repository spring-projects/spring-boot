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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.1.0
 * @see ConditionalOnProperty
 */
class OnPropertyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		List<AnnotationAttributes> allAnnotationAttributes = annotationAttributesFromMultiValueMap(
				metadata.getAllAnnotationAttributes(
						ConditionalOnProperty.class.getName()));
		List<ConditionOutcome> noMatchOutcomes = findNoMatchOutcomes(
				allAnnotationAttributes, context.getEnvironment());
		if (noMatchOutcomes.isEmpty()) {
			return ConditionOutcome.match();
		}
		return ConditionOutcome.noMatch(getCompositeMessage(noMatchOutcomes));
	}

	private List<AnnotationAttributes> annotationAttributesFromMultiValueMap(
			MultiValueMap<String, Object> multiValueMap) {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (Entry<String, List<Object>> entry : multiValueMap.entrySet()) {
			for (int i = 0; i < entry.getValue().size(); i++) {
				Map<String, Object> map;
				if (i < maps.size()) {
					map = maps.get(i);
				}
				else {
					map = new HashMap<String, Object>();
					maps.add(map);
				}
				map.put(entry.getKey(), entry.getValue().get(i));
			}
		}
		List<AnnotationAttributes> annotationAttributes = new ArrayList<AnnotationAttributes>(
				maps.size());
		for (Map<String, Object> map : maps) {
			annotationAttributes.add(AnnotationAttributes.fromMap(map));
		}
		return annotationAttributes;
	}

	private List<ConditionOutcome> findNoMatchOutcomes(
			List<AnnotationAttributes> allAnnotationAttributes,
			PropertyResolver resolver) {
		List<ConditionOutcome> noMatchOutcomes = new ArrayList<ConditionOutcome>(
				allAnnotationAttributes.size());
		for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
			ConditionOutcome outcome = determineOutcome(annotationAttributes, resolver);
			if (!outcome.isMatch()) {
				noMatchOutcomes.add(outcome);
			}
		}
		return noMatchOutcomes;
	}

	private ConditionOutcome determineOutcome(AnnotationAttributes annotationAttributes,
			PropertyResolver resolver) {
		String prefix = annotationAttributes.getString("prefix").trim();
		if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		String havingValue = annotationAttributes.getString("havingValue");
		String[] names = getNames(annotationAttributes);
		boolean relaxedNames = annotationAttributes.getBoolean("relaxedNames");
		boolean matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");

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
			message.append("missing required properties ")
					.append(expandNames(prefix, missingProperties)).append(" ");
		}
		if (!nonMatchingProperties.isEmpty()) {
			String expected = StringUtils.hasLength(havingValue) ? havingValue : "!false";
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
		StringBuilder expanded = new StringBuilder();
		for (String name : names) {
			expanded.append(expanded.length() == 0 ? "" : ", ");
			expanded.append(prefix);
			expanded.append(name);
		}
		return expanded.toString();
	}

	private String getCompositeMessage(List<ConditionOutcome> noMatchOutcomes) {
		StringBuilder message = new StringBuilder();
		for (ConditionOutcome noMatchOutcome : noMatchOutcomes) {
			if (message.length() > 0) {
				message.append(". ");
			}
			message.append(noMatchOutcome.getMessage().trim());
		}
		return message.toString();
	}

}
