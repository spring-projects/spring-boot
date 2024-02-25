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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;
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

	/**
     * Set the ClassLoader to be used for loading bean classes.
     * 
     * @param classLoader the ClassLoader to be used
     */
    @Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
     * Determines if the given metadata reader matches the specified criteria.
     * 
     * @param metadataReader the metadata reader to be checked
     * @param metadataReaderFactory the factory for creating metadata readers
     * @return {@code true} if the metadata reader matches the criteria, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    @Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (hasAnnotation()) {
			return !(include(metadataReader, metadataReaderFactory) && !exclude(metadataReader, metadataReaderFactory));
		}
		return false;
	}

	/**
     * Determines whether to include the given metadata reader based on the specified filters.
     * 
     * @param metadataReader the metadata reader to be evaluated
     * @param metadataReaderFactory the factory for creating new metadata readers
     * @return {@code true} if the metadata reader should be included, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    protected boolean include(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (new FilterAnnotations(this.classLoader, getFilters(FilterType.INCLUDE)).anyMatches(metadataReader,
				metadataReaderFactory)) {
			return true;
		}
		return isUseDefaultFilters() && defaultInclude(metadataReader, metadataReaderFactory);
	}

	/**
     * Determines whether the given metadata reader should be included based on the default includes and component includes.
     * 
     * @param metadataReader the metadata reader to be checked
     * @param metadataReaderFactory the metadata reader factory
     * @return true if the metadata reader should be included, false otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
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

	/**
     * Determines whether to exclude the given metadata reader based on the specified filters.
     *
     * @param metadataReader The metadata reader to be evaluated.
     * @param metadataReaderFactory The factory for creating metadata readers.
     * @return {@code true} if the metadata reader should be excluded, {@code false} otherwise.
     * @throws IOException if an I/O error occurs while reading the metadata.
     */
    protected boolean exclude(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		return new FilterAnnotations(this.classLoader, getFilters(FilterType.EXCLUDE)).anyMatches(metadataReader,
				metadataReaderFactory);
	}

	/**
     * Determines if the given metadata reader represents a type that is either of the specified type or annotated with the specified type.
     * 
     * @param metadataReader the metadata reader to check
     * @param metadataReaderFactory the metadata reader factory to use
     * @param type the type to check against
     * @return {@code true} if the metadata reader represents a type that is either of the specified type or annotated with the specified type, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    @SuppressWarnings("unchecked")
	protected final boolean isTypeOrAnnotated(MetadataReader metadataReader,
			MetadataReaderFactory metadataReaderFactory, Class<?> type) throws IOException {
		AnnotationTypeFilter annotationFilter = new AnnotationTypeFilter((Class<? extends Annotation>) type);
		AssignableTypeFilter typeFilter = new AssignableTypeFilter(type);
		return annotationFilter.match(metadataReader, metadataReaderFactory)
				|| typeFilter.match(metadataReader, metadataReaderFactory);
	}

	/**
     * Returns a boolean value indicating whether this AnnotationCustomizableTypeExcludeFilter has an annotation.
     *
     * @return {@code true} if this AnnotationCustomizableTypeExcludeFilter has an annotation, {@code false} otherwise.
     */
    protected abstract boolean hasAnnotation();

	/**
     * Retrieves an array of filters based on the specified filter type.
     *
     * @param type the filter type to retrieve filters for
     * @return an array of filters of the specified type
     */
    protected abstract Filter[] getFilters(FilterType type);

	/**
     * Returns a boolean value indicating whether to use default filters.
     *
     * @return {@code true} if default filters should be used, {@code false} otherwise.
     */
    protected abstract boolean isUseDefaultFilters();

	/**
     * Returns the default includes for this filter.
     *
     * @return the set of classes to be included by default
     */
    protected abstract Set<Class<?>> getDefaultIncludes();

	/**
     * Returns the set of component classes to include.
     *
     * @return the set of component classes to include
     */
    protected abstract Set<Class<?>> getComponentIncludes();

	protected enum FilterType {

		INCLUDE, EXCLUDE

	}

	/**
     * Compares this AnnotationCustomizableTypeExcludeFilter object to the specified object.
     * 
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AnnotationCustomizableTypeExcludeFilter other = (AnnotationCustomizableTypeExcludeFilter) obj;
		boolean result = hasAnnotation() == other.hasAnnotation();
		for (FilterType filterType : FilterType.values()) {
			result &= ObjectUtils.nullSafeEquals(getFilters(filterType), other.getFilters(filterType));
		}
		result = result && isUseDefaultFilters() == other.isUseDefaultFilters();
		result = result && ObjectUtils.nullSafeEquals(getDefaultIncludes(), other.getDefaultIncludes());
		result = result && ObjectUtils.nullSafeEquals(getComponentIncludes(), other.getComponentIncludes());
		return result;
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the {@code hashCode()} method.
     * 
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + Boolean.hashCode(hasAnnotation());
		for (FilterType filterType : FilterType.values()) {
			result = prime * result + Arrays.hashCode(getFilters(filterType));
		}
		result = prime * result + Boolean.hashCode(isUseDefaultFilters());
		result = prime * result + Objects.hashCode(getDefaultIncludes());
		result = prime * result + Objects.hashCode(getComponentIncludes());
		return result;
	}

}
