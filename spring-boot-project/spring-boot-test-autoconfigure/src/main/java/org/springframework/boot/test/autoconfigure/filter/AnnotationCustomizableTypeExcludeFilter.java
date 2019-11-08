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

package org.springframework.boot.test.autoconfigure.filter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for a {@link TypeExcludeFilter} that can be customized using an
 * annotation.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public abstract class AnnotationCustomizableTypeExcludeFilter extends TypeExcludeFilter
		implements BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (hasAnnotation()) {
			return !(include(metadataReader, metadataReaderFactory) && !exclude(metadataReader, metadataReaderFactory));
		}
		return false;
	}

	protected boolean include(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (new FilterAnnotations(this.classLoader, getFilters(FilterType.INCLUDE)).anyMatches(metadataReader,
				metadataReaderFactory)) {
			return true;
		}
		if (isUseDefaultFilters() && defaultInclude(metadataReader, metadataReaderFactory)) {
			return true;
		}
		return false;
	}

	protected boolean defaultInclude(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		for (Class<?> include : getDefaultIncludes()) {
			if (isTypeOrAnnotated(metadataReader, metadataReaderFactory, include)) {
				return true;
			}
		}
		for (Class<?> component : getComponentIncludes()) {
			if (isTypeOrAnnotated(metadataReader, metadataReaderFactory, component)) {
				return true;
			}
		}
		return false;
	}

	protected boolean exclude(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		return new FilterAnnotations(this.classLoader, getFilters(FilterType.EXCLUDE)).anyMatches(metadataReader,
				metadataReaderFactory);
	}

	@SuppressWarnings("unchecked")
	protected final boolean isTypeOrAnnotated(MetadataReader metadataReader,
			MetadataReaderFactory metadataReaderFactory, Class<?> type) throws IOException {
		AnnotationTypeFilter annotationFilter = new AnnotationTypeFilter((Class<? extends Annotation>) type);
		AssignableTypeFilter typeFilter = new AssignableTypeFilter(type);
		return annotationFilter.match(metadataReader, metadataReaderFactory)
				|| typeFilter.match(metadataReader, metadataReaderFactory);
	}

	protected abstract boolean hasAnnotation();

	protected abstract Filter[] getFilters(FilterType type);

	protected abstract boolean isUseDefaultFilters();

	protected abstract Set<Class<?>> getDefaultIncludes();

	protected abstract Set<Class<?>> getComponentIncludes();

	protected enum FilterType {

		INCLUDE, EXCLUDE

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AnnotationCustomizableTypeExcludeFilter other = (AnnotationCustomizableTypeExcludeFilter) obj;
		boolean result = true;
		result = result && hasAnnotation() == other.hasAnnotation();
		for (FilterType filterType : FilterType.values()) {
			result &= ObjectUtils.nullSafeEquals(getFilters(filterType), other.getFilters(filterType));
		}
		result = result && isUseDefaultFilters() == other.isUseDefaultFilters();
		result = result && ObjectUtils.nullSafeEquals(getDefaultIncludes(), other.getDefaultIncludes());
		result = result && ObjectUtils.nullSafeEquals(getComponentIncludes(), other.getComponentIncludes());
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + Boolean.hashCode(hasAnnotation());
		for (FilterType filterType : FilterType.values()) {
			result = prime * result + ObjectUtils.nullSafeHashCode(getFilters(filterType));
		}
		result = prime * result + Boolean.hashCode(isUseDefaultFilters());
		result = prime * result + ObjectUtils.nullSafeHashCode(getDefaultIncludes());
		result = prime * result + ObjectUtils.nullSafeHashCode(getComponentIncludes());
		return result;
	}

}
