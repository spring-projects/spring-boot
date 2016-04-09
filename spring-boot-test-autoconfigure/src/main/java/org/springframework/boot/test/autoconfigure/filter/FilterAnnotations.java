/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

	public FilterAnnotations(ClassLoader classLoader, Filter[] filters) {
		Assert.notNull(filters, "Filters must not be null");
		this.classLoader = classLoader;
		this.filters = createTypeFilters(filters);
	}

	private List<TypeFilter> createTypeFilters(Filter[] filters) {
		List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
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

	@SuppressWarnings("unchecked")
	private TypeFilter createTypeFilter(FilterType filterType, Class<?> filterClass) {
		switch (filterType) {
		case ANNOTATION:
			Assert.isAssignable(Annotation.class, filterClass,
					"An error occurred while processing a ANNOTATION type filter: ");
			return new AnnotationTypeFilter((Class<Annotation>) filterClass);
		case ASSIGNABLE_TYPE:
			return new AssignableTypeFilter(filterClass);
		case CUSTOM:
			Assert.isAssignable(TypeFilter.class, filterClass,
					"An error occurred while processing a CUSTOM type filter: ");
			return BeanUtils.instantiateClass(filterClass, TypeFilter.class);
		}
		throw new IllegalArgumentException(
				"Filter type not supported with Class value: " + filterType);
	}

	private TypeFilter createTypeFilter(FilterType filterType, String pattern) {
		switch (filterType) {
		case ASPECTJ:
			return new AspectJTypeFilter(pattern, this.classLoader);
		case REGEX:
			return new RegexPatternTypeFilter(Pattern.compile(pattern));
		}
		throw new IllegalArgumentException(
				"Filter type not supported with String pattern: " + filterType);
	}

	@Override
	public Iterator<TypeFilter> iterator() {
		return this.filters.iterator();
	}

	public boolean anyMatches(MetadataReader metadataReader,
			MetadataReaderFactory metadataReaderFactory) throws IOException {
		for (TypeFilter filter : this) {
			if (filter.match(metadataReader, metadataReaderFactory)) {
				return true;
			}
		}
		return false;
	}

}
