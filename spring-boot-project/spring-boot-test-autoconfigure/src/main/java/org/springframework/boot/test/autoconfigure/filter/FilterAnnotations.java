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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Utility to load {@link TypeFilter TypeFilters} from {@link Filter @Filter} annotations.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class FilterAnnotations implements Iterable<TypeFilter> {

	private final ClassLoader classLoader;

	private final List<TypeFilter> filters;

	/**
     * Creates a new instance of the FilterAnnotations class.
     * 
     * @param classLoader the class loader to use for loading classes
     * @param filters an array of filters to apply
     * @throws IllegalArgumentException if the filters parameter is null
     */
    public FilterAnnotations(ClassLoader classLoader, Filter[] filters) {
		Assert.notNull(filters, "Filters must not be null");
		this.classLoader = classLoader;
		this.filters = createTypeFilters(filters);
	}

	/**
     * Creates a list of TypeFilters based on the given array of filters.
     * 
     * @param filters the array of filters to create TypeFilters from
     * @return an unmodifiable list of TypeFilters
     */
    private List<TypeFilter> createTypeFilters(Filter[] filters) {
		List<TypeFilter> typeFilters = new ArrayList<>();
		for (Filter filter : filters) {
			for (Class<?> filterClass : filter.classes()) {
				typeFilters.add(createTypeFilter(filter.type(), filterClass));
			}
			for (String pattern : filter.pattern()) {
				typeFilters.add(createTypeFilter(filter.type(), pattern));
			}
		}
		return Collections.unmodifiableList(typeFilters);
	}

	/**
     * Creates a type filter based on the given filter type and filter class.
     *
     * @param filterType   the type of filter to create
     * @param filterClass  the class to use for the filter
     * @return the created type filter
     * @throws IllegalArgumentException if the filter type is not supported with a class value
     */
    @SuppressWarnings("unchecked")
	private TypeFilter createTypeFilter(FilterType filterType, Class<?> filterClass) {
		return switch (filterType) {
			case ANNOTATION -> {
				Assert.isAssignable(Annotation.class, filterClass,
						"An error occurred while processing an ANNOTATION type filter: ");
				yield new AnnotationTypeFilter((Class<Annotation>) filterClass);
			}
			case ASSIGNABLE_TYPE -> new AssignableTypeFilter(filterClass);
			case CUSTOM -> {
				Assert.isAssignable(TypeFilter.class, filterClass,
						"An error occurred while processing a CUSTOM type filter: ");
				yield BeanUtils.instantiateClass(filterClass, TypeFilter.class);
			}
			default -> throw new IllegalArgumentException("Filter type not supported with Class value: " + filterType);
		};
	}

	/**
     * Creates a TypeFilter based on the given filter type and pattern.
     *
     * @param filterType the type of filter to create
     * @param pattern the pattern to use for filtering
     * @return the created TypeFilter
     * @throws IllegalArgumentException if the filter type is not supported with a string pattern
     */
    private TypeFilter createTypeFilter(FilterType filterType, String pattern) {
		return switch (filterType) {
			case ASPECTJ -> new AspectJTypeFilter(pattern, this.classLoader);
			case REGEX -> new RegexPatternTypeFilter(Pattern.compile(pattern));
			default ->
				throw new IllegalArgumentException("Filter type not supported with String pattern: " + filterType);
		};
	}

	/**
     * Returns an iterator over the elements in this collection.
     *
     * @return an iterator over the elements in this collection
     */
    @Override
	public Iterator<TypeFilter> iterator() {
		return this.filters.iterator();
	}

	/**
     * Checks if any of the filters in the list match the given metadata reader and metadata reader factory.
     * 
     * @param metadataReader the metadata reader to be matched against the filters
     * @param metadataReaderFactory the metadata reader factory used to create metadata readers
     * @return true if any of the filters match the metadata reader, false otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    public boolean anyMatches(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		for (TypeFilter filter : this) {
			if (filter.match(metadataReader, metadataReaderFactory)) {
				return true;
			}
		}
		return false;
	}

}
