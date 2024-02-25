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

package org.springframework.boot.test.autoconfigure.filter;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * {@link AnnotationCustomizableTypeExcludeFilter} that can be used to any test annotation
 * that uses the standard {@code includeFilters}, {@code excludeFilters} and
 * {@code useDefaultFilters} attributes.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @since 2.2.0
 */
public abstract class StandardAnnotationCustomizableTypeExcludeFilter<A extends Annotation>
		extends AnnotationCustomizableTypeExcludeFilter {

	private static final Filter[] NO_FILTERS = {};

	private static final String[] FILTER_TYPE_ATTRIBUTES;
	static {
		FilterType[] filterValues = FilterType.values();
		FILTER_TYPE_ATTRIBUTES = new String[filterValues.length];
		for (int i = 0; i < filterValues.length; i++) {
			FILTER_TYPE_ATTRIBUTES[i] = filterValues[i].name().toLowerCase() + "Filters";
		}
	}

	private final MergedAnnotation<A> annotation;

	/**
     * Constructs a new {@code StandardAnnotationCustomizableTypeExcludeFilter} object with the given test class.
     * 
     * @param testClass the test class to create the filter for
     */
    protected StandardAnnotationCustomizableTypeExcludeFilter(Class<?> testClass) {
		this.annotation = MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
			.get(getAnnotationType());
	}

	/**
     * Returns the merged annotation associated with this StandardAnnotationCustomizableTypeExcludeFilter.
     *
     * @return the merged annotation
     */
    protected final MergedAnnotation<A> getAnnotation() {
		return this.annotation;
	}

	/**
     * Returns a boolean value indicating whether this method has an annotation.
     * 
     * @return {@code true} if this method has an annotation, {@code false} otherwise.
     */
    @Override
	protected boolean hasAnnotation() {
		return this.annotation.isPresent();
	}

	/**
     * Retrieves the filters of the specified type.
     * 
     * @param type the type of filters to retrieve
     * @return an array of filters of the specified type, or an empty array if no filters of the specified type are found
     */
    @Override
	protected Filter[] getFilters(FilterType type) {
		return this.annotation.getValue(FILTER_TYPE_ATTRIBUTES[type.ordinal()], Filter[].class).orElse(NO_FILTERS);
	}

	/**
     * Returns a boolean value indicating whether to use default filters.
     *
     * @return {@code true} if default filters should be used, {@code false} otherwise.
     */
    @Override
	protected boolean isUseDefaultFilters() {
		return this.annotation.getValue("useDefaultFilters", Boolean.class).orElse(false);
	}

	/**
     * Returns the default set of classes to be included.
     * 
     * @return an empty set of classes
     */
    @Override
	protected Set<Class<?>> getDefaultIncludes() {
		return Collections.emptySet();
	}

	/**
     * Returns an empty set of classes to include as components.
     * 
     * @return an empty set of classes to include as components
     */
    @Override
	protected Set<Class<?>> getComponentIncludes() {
		return Collections.emptySet();
	}

	/**
     * Returns the annotation type of the current instance.
     * 
     * @return the annotation type of the current instance
     */
    @SuppressWarnings("unchecked")
	protected Class<A> getAnnotationType() {
		ResolvableType type = ResolvableType.forClass(StandardAnnotationCustomizableTypeExcludeFilter.class,
				getClass());
		return (Class<A>) type.resolveGeneric();
	}

}
