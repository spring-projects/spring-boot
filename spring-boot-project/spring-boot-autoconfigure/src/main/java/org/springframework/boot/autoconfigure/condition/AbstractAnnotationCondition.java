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

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for annotation conditions.
 *
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.4.0
 */
public abstract class AbstractAnnotationCondition implements Condition {

	protected static final String VALUE_ATTRIBUTE = "value";

	private Map<String, Object> attributes;

	protected abstract Class<? extends Annotation> annotationClass();

	protected Map<String, Object> getAnnotationAttributes(AnnotatedTypeMetadata metadata) {
		if (this.attributes == null) {
			final Map<String, Object> attributes = metadata.getAnnotationAttributes(annotationClass().getName());
			assert attributes != null;
			this.attributes = attributes;
		}
		return this.attributes;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getAttribute(AnnotatedTypeMetadata metadata, String attributeName) {
		return (T) getAnnotationAttributes(metadata).get(attributeName);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getValue(AnnotatedTypeMetadata metadata) {
		return (T) getAttribute(metadata, VALUE_ATTRIBUTE);
	}

	protected Map<String, Object> getBeansWithAnnotation(ConditionContext context,
			Class<? extends Annotation> annotationClass) {
		return Objects.requireNonNull(context.getBeanFactory()).getBeansWithAnnotation(annotationClass);
	}

}
