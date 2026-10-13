/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.beanvalidation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodValidationExcludeFilter}.
 *
 * @author Andy Wilkinson
 */
class MethodValidationExcludeFilterTests {

	@Test
	void byAnnotationWhenClassIsAnnotatedExcludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
		assertThat(filter.isExcluded(Annotated.class)).isTrue();
	}

	@Test
	void byAnnotationWhenClassIsNotAnnotatedIncludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
		assertThat(filter.isExcluded(Plain.class)).isFalse();
	}

	@Test
	void byAnnotationWhenSuperclassIsAnnotatedWithInheritedAnnotationExcludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
		assertThat(filter.isExcluded(AnnotatedSuperClass.class)).isTrue();
	}

	@Test
	void byAnnotationWithDirectSearchStrategyWhenSuperclassIsAnnotatedWithInheritedAnnotationIncludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class,
				SearchStrategy.DIRECT);
		assertThat(filter.isExcluded(AnnotatedSuperClass.class)).isFalse();
	}

	static class Plain {

	}

	@Indicator
	static class Annotated {

	}

	@Inherited
	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator {

	}

	static class AnnotatedSuperClass extends Annotated {

	}

}
