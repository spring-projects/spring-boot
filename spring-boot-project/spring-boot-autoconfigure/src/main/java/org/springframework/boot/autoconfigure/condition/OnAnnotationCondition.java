/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for the presence or absence of specific annotations.
 *
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.4.0
 */
public class OnAnnotationCondition extends AbstractAnnotationCondition {

	private static final String CONDITION_TYPE_ATTRIBUTE = "conditionType";

	private final Log log = LogFactory.getLog(getClass());

	@Override
	public Class<? extends Annotation> annotationClass() {
		return ConditionalOnAnnotation.class;
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

		final Class<? extends Annotation>[] annotatedClasses = getValue(metadata);

		if (ArrayUtils.isEmpty(annotatedClasses)) {
			this.log.warn("@ConditionalOnAnnotation should be annotated with "
					+ "minimum 1 Annotation type classes. Making the condition as true.");
			return true;
		}

		final ConditionalOnAnnotation.ConditionType conditionType = getAttribute(metadata, CONDITION_TYPE_ATTRIBUTE);

		return (conditionType == ConditionalOnAnnotation.ConditionType.OR)
				? onOrConditionType(context, annotatedClasses) : onAndConditionType(context, annotatedClasses);
	}

	protected boolean onOrConditionType(ConditionContext context, Class<? extends Annotation>[] annotatedClasses) {
		for (Class<? extends Annotation> annotatedClass : annotatedClasses) {
			final Map<String, Object> candidates = getBeansWithAnnotation(context, annotatedClass);

			// Return true if any one of the annotation classes is present
			if (!candidates.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	protected boolean onAndConditionType(ConditionContext context, Class<? extends Annotation>[] annotatedClasses) {
		for (Class<? extends Annotation> annotatedClass : annotatedClasses) {
			final Map<String, Object> candidates = getBeansWithAnnotation(context, annotatedClass);

			// Return false if any one of the annotation classes is not present
			if (candidates.isEmpty()) {
				return false;
			}
		}
		return true;
	}

}
