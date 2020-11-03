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

package org.springframework.boot.test.context.filter;

import java.io.IOException;

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

	private static final String[] CLASS_ANNOTATIONS = { "org.junit.runner.RunWith",
			"org.junit.jupiter.api.extension.ExtendWith", "org.junit.platform.commons.annotation.Testable",
			"org.testng.annotations.Test" };

	private static final String[] METHOD_ANNOTATIONS = { "org.junit.Test",
			"org.junit.platform.commons.annotation.Testable", "org.testng.annotations.Test" };

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

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	private boolean isTestConfiguration(MetadataReader metadataReader) {
		return (metadataReader.getAnnotationMetadata().isAnnotated(TestComponent.class.getName()));
	}

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

}
