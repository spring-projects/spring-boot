/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure;

import java.lang.annotation.Annotation;

import org.jspecify.annotations.Nullable;

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
		Class<T> annotationType = (Class<T>) ResolvableType.forClass(getClass())
			.as(TestSliceTestContextBootstrapper.class)
			.getGeneric(0)
			.resolve();
		Assert.notNull(annotationType, "'%s' doesn't contain type parameter of '%s'".formatted(getClass().getName(),
				TestSliceTestContextBootstrapper.class.getName()));
		this.annotationType = annotationType;
	}

	@Override
	protected String @Nullable [] getProperties(Class<?> testClass) {
		MergedAnnotation<T> annotation = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
			.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
			.from(testClass)
			.get(this.annotationType);
		return annotation.isPresent() ? annotation.getStringArray("properties") : null;
	}

}
