/*
 * Copyright 2012-2020 the original author or authors.
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
	 * Factory method to crate a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation.
	 * @param annotationType the annotation to check
	 * @return a {@link MethodValidationExcludeFilter} instance
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType) {
		return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
	}

	/**
	 * Factory method to crate a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation.
	 * @param annotationType the annotation to check
	 * @param searchStrategy the annotation search strategy
	 * @return a {@link MethodValidationExcludeFilter} instance
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType,
			SearchStrategy searchStrategy) {
		return (type) -> MergedAnnotations.from(type, SearchStrategy.SUPERCLASS).isPresent(annotationType);
	}

}
