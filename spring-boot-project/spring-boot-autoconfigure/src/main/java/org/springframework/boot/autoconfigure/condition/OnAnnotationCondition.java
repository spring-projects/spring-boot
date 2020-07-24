package org.springframework.boot.autoconfigure.condition;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.3.2
 */
class OnAnnotationCondition extends AbstractAnnotationCondition {

    private static final Logger log = LoggerFactory.getLogger(OnAnnotationCondition.class);

    private static final String CONDITION_TYPE_ATTRIBUTE = "conditionType";

    @Override
    public Class<? extends Annotation> annotationClass() {
        return ConditionalOnAnnotation.class;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        final Class<? extends Annotation>[] annotatedClasses = getValue(metadata);

        if(ArrayUtils.isEmpty(annotatedClasses)) {
			log.warn("@ConditionalOnAnnotation should be annotated with " +
					"minimum 1 Annotation type classes. Making the condition as true.");
			return true;
        }

		final ConditionalOnAnnotation.ConditionType conditionType = getAttribute(metadata, CONDITION_TYPE_ATTRIBUTE);

        return conditionType == ConditionalOnAnnotation.ConditionType.OR
				? onOrConditionType(context, annotatedClasses)
				: onAndConditionType(context, annotatedClasses);
    }

    protected boolean onOrConditionType(ConditionContext context, Class<? extends Annotation>[] annotatedClasses) {
        for (Class<? extends Annotation> annotatedClass : annotatedClasses) {
            final Map<String, Object> candidates = getBeansWithAnnotation(context, annotatedClass);

            // Return true if any one of the annotation classes is present
            if(!candidates.isEmpty()) {
                return true;
            }
        }
        return false;
    }

	protected boolean onAndConditionType(ConditionContext context, Class<? extends Annotation>[] annotatedClasses) {
        for (Class<? extends Annotation> annotatedClass : annotatedClasses) {
            final Map<String, Object> candidates = getBeansWithAnnotation(context, annotatedClass);

            // Return false if any one of the annotation classes is not present
            if(candidates.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
