/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required {@link Annotation}.
 *
 * @author Feng, Liu
 */
public class OnAnnotationCondition extends SpringBootCondition {

	@SuppressWarnings("unchecked")
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAnnotation.class.getName());
		var annotationType = (Class<? extends Annotation>) attributes.get("value");
		return getMatchOutcome(context, annotationType);
	}

	private ConditionOutcome getMatchOutcome(ConditionContext context, Class<? extends Annotation> annotationType) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnAnnotation.class);
		if (annotationType == null) {
			return ConditionOutcome.noMatch(message.didNotFind(annotationType.getName()).atAll());
		}
		var result = Arrays.stream(Objects.requireNonNull(context.getBeanFactory()).getBeanDefinitionNames())
			.map(beanName -> context.getBeanFactory().getType(beanName))
			.filter(Objects::nonNull)
			.anyMatch(clazz -> clazz.isAnnotationPresent(annotationType));
		if (result) {
			return ConditionOutcome.match(message.foundExactly(annotationType.getName()));
		}
		return ConditionOutcome.noMatch(message.didNotFind(annotationType.getName()).atAll());
	}

}
