/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.Order;
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
 * @author Andy Wilkinson
 * @see ConditionalOnProperty
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertyCondition extends SpringBootCondition {

	/**
     * Determines the match outcome for the given condition context and annotated type metadata.
     * 
     * @param context the condition context
     * @param metadata the annotated type metadata
     * @return the condition outcome
     */
    @Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		List<AnnotationAttributes> allAnnotationAttributes = metadata.getAnnotations()
			.stream(ConditionalOnProperty.class.getName())
			.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
			.map(MergedAnnotation::asAnnotationAttributes)
			.toList();
		List<ConditionMessage> noMatch = new ArrayList<>();
		List<ConditionMessage> match = new ArrayList<>();
		for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
			ConditionOutcome outcome = determineOutcome(annotationAttributes, context.getEnvironment());
			(outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
		}
		if (!noMatch.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
		}
		return ConditionOutcome.match(ConditionMessage.of(match));
	}

	/**
     * Determines the outcome of the condition based on the provided annotation attributes and property resolver.
     * 
     * @param annotationAttributes the annotation attributes containing the properties to check
     * @param resolver the property resolver to use for resolving property values
     * @return the condition outcome
     */
    private ConditionOutcome determineOutcome(AnnotationAttributes annotationAttributes, PropertyResolver resolver) {
		Spec spec = new Spec(annotationAttributes);
		List<String> missingProperties = new ArrayList<>();
		List<String> nonMatchingProperties = new ArrayList<>();
		spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
		if (!missingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
				.didNotFind("property", "properties")
				.items(Style.QUOTE, missingProperties));
		}
		if (!nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
				.found("different value in property", "different value in properties")
				.items(Style.QUOTE, nonMatchingProperties));
		}
		return ConditionOutcome
			.match(ConditionMessage.forCondition(ConditionalOnProperty.class, spec).because("matched"));
	}

	/**
     * Spec class.
     */
    private static class Spec {

		private final String prefix;

		private final String havingValue;

		private final String[] names;

		private final boolean matchIfMissing;

		/**
         * Initializes a new instance of the Spec class with the provided annotation attributes.
         * 
         * @param annotationAttributes the annotation attributes to initialize the Spec with
         */
        Spec(AnnotationAttributes annotationAttributes) {
			String prefix = annotationAttributes.getString("prefix").trim();
			if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
				prefix = prefix + ".";
			}
			this.prefix = prefix;
			this.havingValue = annotationAttributes.getString("havingValue");
			this.names = getNames(annotationAttributes);
			this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
		}

		/**
         * Retrieves the names from the given annotation attributes.
         * 
         * @param annotationAttributes the map of annotation attributes
         * @return an array of names
         * @throws IllegalStateException if neither the "value" nor the "name" attribute is specified
         * @throws IllegalStateException if both the "value" and the "name" attributes are specified
         */
        private String[] getNames(Map<String, Object> annotationAttributes) {
			String[] value = (String[]) annotationAttributes.get("value");
			String[] name = (String[]) annotationAttributes.get("name");
			Assert.state(value.length > 0 || name.length > 0,
					"The name or value attribute of @ConditionalOnProperty must be specified");
			Assert.state(value.length == 0 || name.length == 0,
					"The name and value attributes of @ConditionalOnProperty are exclusive");
			return (value.length > 0) ? value : name;
		}

		/**
         * Collects properties from the given PropertyResolver and adds them to the appropriate lists.
         * 
         * @param resolver the PropertyResolver to collect properties from
         * @param missing a list to store missing properties
         * @param nonMatching a list to store properties that do not match the specified value
         */
        private void collectProperties(PropertyResolver resolver, List<String> missing, List<String> nonMatching) {
			for (String name : this.names) {
				String key = this.prefix + name;
				if (resolver.containsProperty(key)) {
					if (!isMatch(resolver.getProperty(key), this.havingValue)) {
						nonMatching.add(name);
					}
				}
				else {
					if (!this.matchIfMissing) {
						missing.add(name);
					}
				}
			}
		}

		/**
         * Checks if the given value matches the required value.
         * 
         * @param value          the value to be checked
         * @param requiredValue  the required value to be matched against
         * @return               true if the value matches the required value, false otherwise
         */
        private boolean isMatch(String value, String requiredValue) {
			if (StringUtils.hasLength(requiredValue)) {
				return requiredValue.equalsIgnoreCase(value);
			}
			return !"false".equalsIgnoreCase(value);
		}

		/**
         * Returns a string representation of the object.
         * The string representation includes the prefix, names, and havingValue of the object.
         * If there is only one name, it is appended directly after the prefix.
         * If there are multiple names, they are enclosed in square brackets and separated by commas.
         * If a havingValue is present, it is appended after an equals sign.
         * The resulting string is enclosed in parentheses.
         *
         * @return a string representation of the object
         */
        @Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("(");
			result.append(this.prefix);
			if (this.names.length == 1) {
				result.append(this.names[0]);
			}
			else {
				result.append("[");
				result.append(StringUtils.arrayToCommaDelimitedString(this.names));
				result.append("]");
			}
			if (StringUtils.hasLength(this.havingValue)) {
				result.append("=").append(this.havingValue);
			}
			result.append(")");
			return result.toString();
		}

	}

}
