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

package org.springframework.boot.test.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.Assert;

/**
 * Base class for test slice {@link TestContextBootstrapper test context bootstrappers}.
 *
 * @param <T> the test slice annotation
 * @author Yanming Zhou
 * @since 4.0.0
 */
public abstract class TestSliceTestContextBootstrapper<T extends Annotation> extends SpringBootTestContextBootstrapper {

	private final Class<T> annotationType;

	@SuppressWarnings("unchecked")
	protected TestSliceTestContextBootstrapper() {
		this.annotationType = (Class<T>) ResolvableType.forClass(getClass())
			.as(TestSliceTestContextBootstrapper.class)
			.getGeneric(0)
			.resolve();
		Assert.notNull(this.annotationType, "'%s' doesn't contain type parameter of '%s'"
			.formatted(getClass().getName(), TestSliceTestContextBootstrapper.class.getName()));
	}

	@Override
	protected String[] getProperties(Class<?> testClass) {
		MergedAnnotation<T> annotation = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
			.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
			.from(testClass)
			.get(this.annotationType);
		return annotation.isPresent() ? annotation.getStringArray("properties") : null;
	}

}
