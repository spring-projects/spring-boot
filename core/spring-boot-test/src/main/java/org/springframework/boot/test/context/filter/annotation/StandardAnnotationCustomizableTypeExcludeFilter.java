/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.context.filter.annotation;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;

/**
 * {@link AnnotationCustomizableTypeExcludeFilter} that can be used to any test annotation
 * that uses the standard {@code includeFilters}, {@code excludeFilters} and
 * {@code useDefaultFilters} attributes.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract class StandardAnnotationCustomizableTypeExcludeFilter<A extends Annotation>
		extends AnnotationCustomizableTypeExcludeFilter {

	private static final Filter[] NO_FILTERS = {};

	private static final String[] FILTER_TYPE_ATTRIBUTES;
	static {
		FilterType[] filterValues = FilterType.values();
		FILTER_TYPE_ATTRIBUTES = new String[filterValues.length];
		for (int i = 0; i < filterValues.length; i++) {
			FILTER_TYPE_ATTRIBUTES[i] = filterValues[i].name().toLowerCase(Locale.ROOT) + "Filters";
		}
	}

	private final MergedAnnotation<A> annotation;

	protected StandardAnnotationCustomizableTypeExcludeFilter(Class<?> testClass) {
		this.annotation = MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
			.get(getAnnotationType());
	}

	protected final MergedAnnotation<A> getAnnotation() {
		return this.annotation;
	}

	@Override
	protected boolean hasAnnotation() {
		return this.annotation.isPresent();
	}

	@Override
	protected Filter[] getFilters(FilterType type) {
		return this.annotation.getValue(FILTER_TYPE_ATTRIBUTES[type.ordinal()], Filter[].class).orElse(NO_FILTERS);
	}

	@Override
	protected boolean isUseDefaultFilters() {
		return this.annotation.getValue("useDefaultFilters", Boolean.class).orElse(false);
	}

	@Override
	protected final Set<Class<?>> getDefaultIncludes() {
		Set<Class<?>> defaultIncludes = new HashSet<>();
		defaultIncludes.addAll(getKnownIncludes());
		defaultIncludes.addAll(TypeIncludes.load(this.annotation.getType(), getClass().getClassLoader()).getIncludes());
		return defaultIncludes;
	}

	protected Set<Class<?>> getKnownIncludes() {
		return Collections.emptySet();
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	protected Class<A> getAnnotationType() {
		ResolvableType type = ResolvableType.forClass(StandardAnnotationCustomizableTypeExcludeFilter.class,
				getClass());
		Class<A> generic = (Class<A>) type.resolveGeneric();
		Assert.state(generic != null, "'generic' must not be null");
		return generic;
	}

}
