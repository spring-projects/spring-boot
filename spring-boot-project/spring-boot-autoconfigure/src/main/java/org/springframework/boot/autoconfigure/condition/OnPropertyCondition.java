/*
 * Copyright 2012-2025 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnProperty
 * @see ConditionalOnBooleanProperty
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MergedAnnotations mergedAnnotations = metadata.getAnnotations();
		List<MergedAnnotation<Annotation>> annotations = stream(mergedAnnotations).toList();
		List<ConditionMessage> noMatch = new ArrayList<>();
		List<ConditionMessage> match = new ArrayList<>();
		for (MergedAnnotation<Annotation> annotation : annotations) {
			ConditionOutcome outcome = determineOutcome(annotation, context.getEnvironment());
			(outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
		}
		if (!noMatch.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
		}
		return ConditionOutcome.match(ConditionMessage.of(match));
	}

	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations mergedAnnotations) {
		return Stream.concat(stream(mergedAnnotations, ConditionalOnProperty.class, ConditionalOnProperties.class),
				stream(mergedAnnotations, ConditionalOnBooleanProperty.class, ConditionalOnBooleanProperties.class));
	}

	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations mergedAnnotations,
			Class<? extends Annotation> type, Class<? extends Annotation> containerType) {
		return Stream.concat(stream(mergedAnnotations, type), streamRepeated(mergedAnnotations, type, containerType));
	}

	private Stream<MergedAnnotation<Annotation>> streamRepeated(MergedAnnotations mergedAnnotations,
			Class<? extends Annotation> type, Class<? extends Annotation> containerType) {
		return stream(mergedAnnotations, containerType).flatMap((container) -> streamRepeated(container, type));
	}

	@SuppressWarnings("unchecked")
	private Stream<MergedAnnotation<Annotation>> streamRepeated(MergedAnnotation<Annotation> container,
			Class<? extends Annotation> type) {
		MergedAnnotation<? extends Annotation>[] repeated = container.getAnnotationArray(MergedAnnotation.VALUE, type);
		return Arrays.stream((MergedAnnotation<Annotation>[]) repeated);
	}

	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations annotations,
			Class<? extends Annotation> containerType) {
		return annotations.stream(containerType.getName())
			.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes));
	}

	private ConditionOutcome determineOutcome(MergedAnnotation<Annotation> annotation, PropertyResolver resolver) {
		Class<Annotation> annotationType = annotation.getType();
		Spec spec = new Spec(annotationType, annotation.asAnnotationAttributes());
		List<String> missingProperties = new ArrayList<>();
		List<String> nonMatchingProperties = new ArrayList<>();
		spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
		if (!missingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(annotationType, spec)
				.didNotFind("property", "properties")
				.items(Style.QUOTE, missingProperties));
		}
		if (!nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(annotationType, spec)
				.found("different value in property", "different value in properties")
				.items(Style.QUOTE, nonMatchingProperties));
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(annotationType, spec).because("matched"));
	}

	private static class Spec {

		private final Class<? extends Annotation> annotationType;

		private final String prefix;

		private final String[] names;

		private final String havingValue;

		private final boolean matchIfMissing;

		Spec(Class<? extends Annotation> annotationType, AnnotationAttributes annotationAttributes) {
			this.annotationType = annotationType;
			this.prefix = (!annotationAttributes.containsKey("prefix")) ? "" : getPrefix(annotationAttributes);
			this.names = getNames(annotationAttributes);
			this.havingValue = annotationAttributes.get("havingValue").toString();
			this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
		}

		private String getPrefix(AnnotationAttributes annotationAttributes) {
			String prefix = annotationAttributes.getString("prefix").trim();
			if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
				prefix = prefix + ".";
			}
			return prefix;
		}

		private String[] getNames(AnnotationAttributes annotationAttributes) {
			String[] value = (String[]) annotationAttributes.get("value");
			String[] name = (String[]) annotationAttributes.get("name");
			Assert.state(value.length > 0 || name.length > 0,
					() -> "The name or value attribute of @%s must be specified"
						.formatted(ClassUtils.getShortName(this.annotationType)));
			Assert.state(value.length == 0 || name.length == 0,
					() -> "The name and value attributes of @%s are exclusive"
						.formatted(ClassUtils.getShortName(this.annotationType)));
			return (value.length > 0) ? value : name;
		}

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

		private boolean isMatch(String value, String requiredValue) {
			if (StringUtils.hasLength(requiredValue)) {
				return requiredValue.equalsIgnoreCase(value);
			}
			return !"false".equalsIgnoreCase(value);
		}

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
