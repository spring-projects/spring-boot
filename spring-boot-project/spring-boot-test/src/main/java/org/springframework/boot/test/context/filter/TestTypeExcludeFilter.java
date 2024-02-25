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

package org.springframework.boot.test.context.filter;

import java.io.IOException;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * {@link TypeExcludeFilter} to exclude classes annotated with
 * {@link TestComponent @TestComponent} as well as inner-classes of tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TestTypeExcludeFilter extends TypeExcludeFilter {

	private static final String BEAN_NAME = TestTypeExcludeFilter.class.getName();

	private static final String[] CLASS_ANNOTATIONS = { "org.junit.runner.RunWith",
			"org.junit.jupiter.api.extension.ExtendWith", "org.junit.platform.commons.annotation.Testable",
			"org.testng.annotations.Test" };

	private static final String[] METHOD_ANNOTATIONS = { "org.junit.Test",
			"org.junit.platform.commons.annotation.Testable", "org.testng.annotations.Test" };

	private static final TestTypeExcludeFilter INSTANCE = new TestTypeExcludeFilter();

	/**
     * Determines if the given metadata reader matches the specified criteria.
     * 
     * @param metadataReader The metadata reader to be matched.
     * @param metadataReaderFactory The metadata reader factory used to obtain metadata readers.
     * @return {@code true} if the metadata reader matches the criteria, {@code false} otherwise.
     * @throws IOException if an I/O error occurs while reading the metadata.
     */
    @Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (isTestConfiguration(metadataReader)) {
			return true;
		}
		if (isTestClass(metadataReader)) {
			return true;
		}
		String enclosing = metadataReader.getClassMetadata().getEnclosingClassName();
		if (enclosing != null) {
			try {
				if (match(metadataReaderFactory.getMetadataReader(enclosing), metadataReaderFactory)) {
					return true;
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return false;
	}

	/**
     * Compares this object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the specified object is of the same class as this object, {@code false} otherwise
     */
    @Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass());
	}

	/**
     * Returns a hash code value for the object. This method overrides the hashCode() method in the Object class.
     * 
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
     * Determines if the given metadata reader represents a test configuration.
     * 
     * @param metadataReader the metadata reader to check
     * @return {@code true} if the metadata reader is annotated with {@link TestComponent}, {@code false} otherwise
     */
    private boolean isTestConfiguration(MetadataReader metadataReader) {
		return (metadataReader.getAnnotationMetadata().isAnnotated(TestComponent.class.getName()));
	}

	/**
     * Checks if the given metadata reader represents a test class.
     * 
     * @param metadataReader the metadata reader to check
     * @return true if the metadata reader represents a test class, false otherwise
     */
    private boolean isTestClass(MetadataReader metadataReader) {
		for (String annotation : CLASS_ANNOTATIONS) {
			if (metadataReader.getAnnotationMetadata().hasAnnotation(annotation)) {
				return true;
			}

		}
		for (String annotation : METHOD_ANNOTATIONS) {
			if (metadataReader.getAnnotationMetadata().hasAnnotatedMethods(annotation)) {
				return true;
			}
		}
		return false;
	}

	/**
     * Registers the TestTypeExcludeFilter instance with the given ConfigurableListableBeanFactory.
     * If the bean factory does not already contain a singleton with the specified bean name,
     * the TestTypeExcludeFilter instance is registered as a singleton with the bean factory.
     * 
     * @param beanFactory the ConfigurableListableBeanFactory to register the TestTypeExcludeFilter with
     */
    static void registerWith(ConfigurableListableBeanFactory beanFactory) {
		if (!beanFactory.containsSingleton(BEAN_NAME)) {
			beanFactory.registerSingleton(BEAN_NAME, INSTANCE);
		}
	}

}
