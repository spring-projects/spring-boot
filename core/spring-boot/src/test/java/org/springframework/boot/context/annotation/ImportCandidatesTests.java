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
