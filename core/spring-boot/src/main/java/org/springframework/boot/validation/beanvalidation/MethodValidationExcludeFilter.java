/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.beanvalidation;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * A filter for excluding types from method validation.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 * @see FilteredMethodValidationPostProcessor
 */
public interface MethodValidationExcludeFilter {

	/**
	 * Evaluate whether to exclude the given {@code type} from method validation.
	 * @param type the type to evaluate
	 * @return {@code true} to exclude the type from method validation, otherwise
	 * {@code false}.
	 */
	boolean isExcluded(Class<?> type);

	/**
	 * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation found using an {@link SearchStrategy#INHERITED_ANNOTATIONS
	 * inherited annotations search strategy}.
	 * @param annotationType the annotation to check
	 * @return a {@link MethodValidationExcludeFilter} instance
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType) {
		return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
	}

	/**
	 * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation found using the given search strategy.
	 * @param annotationType the annotation to check
	 * @param searchStrategy the annotation search strategy
	 * @return a {@link MethodValidationExcludeFilter} instance
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType,
			SearchStrategy searchStrategy) {
		return (type) -> MergedAnnotations.from(type, searchStrategy).isPresent(annotationType);
	}

}
