/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportCandidates}.
 *
 * @author Moritz Halbritter
 */
class ImportCandidatesTests {

	private static final String IMPORTS_FILE = "META-INF/spring/org.springframework.boot.context.annotation.ImportCandidatesTests$TestAnnotation.imports";

	@Test
	@WithResource(name = IMPORTS_FILE, content = """
			# A comment spanning a complete line
			class1

			class2 # with comment at the end
			   # Comment with some whitespace in front
			class3

			""")
	void loadReadsFromClasspathFile() {
		ImportCandidates candidates = ImportCandidates.load(TestAnnotation.class,
				Thread.currentThread().getContextClassLoader());
		assertThat(candidates).containsExactly("class1", "class2", "class3");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAnnotation {

	}

}
