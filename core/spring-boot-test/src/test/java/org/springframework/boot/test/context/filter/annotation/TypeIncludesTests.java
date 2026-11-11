/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeIncludes}.
 *
 * @author Moritz Halbritter
 */
class TypeIncludesTests {

	private static final String IMPORTS_FILE = "META-INF/spring/org.springframework.boot.test.context.filter.annotation.TypeIncludesTests$TestAnnotation.includes";

	@Test
	@WithResource(name = IMPORTS_FILE, content = """
			org.springframework.boot.test.context.filter.annotation.TypeIncludesTests
			org.springframework.boot.test.context.filter.annotation.DoesNotExist
			""")
	void loadReadsFromClasspathFileIgnoringNonExistentIncludes() {
		TypeIncludes candidates = TypeIncludes.load(TestAnnotation.class,
				Thread.currentThread().getContextClassLoader());
		assertThat(candidates).containsExactly(TypeIncludesTests.class);
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAnnotation {

	}

}
