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
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to support {@link TypeExcludeFilters @TypeExcludeFilters}.
 *
 * @author Phillip Webb
 * @see TypeExcludeFilters
 */
class TypeExcludeFiltersContextCustomizer implements ContextCustomizer {

	private static final String EXCLUDE_FILTER_BEAN_NAME = TypeExcludeFilters.class.getName();

	private final Set<TypeExcludeFilter> filters;

	/**
     * Creates a new instance of TypeExcludeFiltersContextCustomizer with the specified test class and filter classes.
     * 
     * @param testClass the test class for which the type exclude filters are being customized
     * @param filterClasses the set of filter classes to be instantiated as type exclude filters
     */
    TypeExcludeFiltersContextCustomizer(Class<?> testClass, Set<Class<? extends TypeExcludeFilter>> filterClasses) {
		this.filters = instantiateTypeExcludeFilters(testClass, filterClasses);
	}

	/**
     * Instantiates a set of TypeExcludeFilter objects based on the provided filter classes.
     * 
     * @param testClass the test class for which the filters are being instantiated
     * @param filterClasses the classes of the filters to be instantiated
     * @return an unmodifiable set of TypeExcludeFilter objects
     */
    private Set<TypeExcludeFilter> instantiateTypeExcludeFilters(Class<?> testClass,
			Set<Class<? extends TypeExcludeFilter>> filterClasses) {
		Set<TypeExcludeFilter> filters = new LinkedHashSet<>();
		for (Class<? extends TypeExcludeFilter> filterClass : filterClasses) {
			filters.add(instantiateTypeExcludeFilter(testClass, filterClass));
		}
		return Collections.unmodifiableSet(filters);
	}

	/**
     * Instantiates a TypeExcludeFilter using the provided testClass and filterClass.
     * 
     * @param testClass the test class to be used in the constructor of the TypeExcludeFilter
     * @param filterClass the class of the TypeExcludeFilter to be instantiated
     * @return the instantiated TypeExcludeFilter
     * @throws IllegalStateException if unable to create the filter for the specified filterClass
     */
    private TypeExcludeFilter instantiateTypeExcludeFilter(Class<?> testClass, Class<?> filterClass) {
		try {
			Constructor<?> constructor = getTypeExcludeFilterConstructor(filterClass);
			ReflectionUtils.makeAccessible(constructor);
			if (constructor.getParameterCount() == 1) {
				return (TypeExcludeFilter) constructor.newInstance(testClass);
			}
			return (TypeExcludeFilter) constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create filter for " + filterClass, ex);
		}
	}

	/**
     * Compares this TypeExcludeFiltersContextCustomizer with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the specified object is equal to this TypeExcludeFiltersContextCustomizer, {@code false} otherwise
     */
    @Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass())
				&& this.filters.equals(((TypeExcludeFiltersContextCustomizer) obj).filters);
	}

	/**
     * Returns the hash code value for this TypeExcludeFiltersContextCustomizer object.
     * The hash code is generated based on the filters associated with this object.
     *
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.filters.hashCode();
	}

	/**
     * Customize the application context by registering a type exclude filter if the filters list is not empty.
     * 
     * @param context the configurable application context
     * @param mergedContextConfiguration the merged context configuration
     */
    @Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (!this.filters.isEmpty()) {
			context.getBeanFactory().registerSingleton(EXCLUDE_FILTER_BEAN_NAME, createDelegatingTypeExcludeFilter());
		}
	}

	/**
     * Creates a delegating TypeExcludeFilter.
     * 
     * @return The created TypeExcludeFilter.
     */
    private TypeExcludeFilter createDelegatingTypeExcludeFilter() {
		return new TypeExcludeFilter() {

			@Override
			public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
					throws IOException {
				for (TypeExcludeFilter filter : TypeExcludeFiltersContextCustomizer.this.filters) {
					if (filter.match(metadataReader, metadataReaderFactory)) {
						return true;
					}
				}
				return false;
			}

		};
	}

	/**
     * Returns the constructor of the given type that excludes filters.
     * 
     * @param type the type for which to retrieve the constructor
     * @return the constructor of the given type that excludes filters
     * @throws NoSuchMethodException if the constructor cannot be found
     */
    private Constructor<?> getTypeExcludeFilterConstructor(Class<?> type) throws NoSuchMethodException {
		try {
			return type.getDeclaredConstructor(Class.class);
		}
		catch (Exception ex) {
			return type.getDeclaredConstructor();
		}
	}

}
