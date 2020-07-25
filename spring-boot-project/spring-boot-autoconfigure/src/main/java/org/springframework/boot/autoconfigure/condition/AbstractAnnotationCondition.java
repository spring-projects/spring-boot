package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.3.2
 */
abstract class AbstractAnnotationCondition implements Condition {

	protected static final String VALUE_ATTRIBUTE = "value";

	private Map<String, Object> attributes;

	protected abstract Class<? extends Annotation> annotationClass();

	protected Map<String, Object> getAnnotationAttributes(AnnotatedTypeMetadata metadata) {
		if (attributes == null) {
			final Map<String, Object> attributes = metadata.getAnnotationAttributes(annotationClass().getName());
			assert attributes != null;
			this.attributes = attributes;
		}
		return attributes;
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
