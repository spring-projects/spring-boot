/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

/**
 * Tests for {@link AutoConfigurationImportsAnnotationProcessor}.
 *
 * @author Scott Frederick
 * @author Stephane Nicoll
 */
class AutoConfigurationImportsAnnotationProcessorTests {

	@TempDir
	File tempDir;

	private TestCompiler compiler;

	@BeforeEach
	void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.tempDir);
	}

	@Test
	void annotatedClasses() {
		File generatedFile = generateAnnotatedClasses(TestAutoConfigurationOnlyConfiguration.class,
				TestAutoConfigurationConfiguration.class);
		assertThat(generatedFile).exists().isFile();
		assertThat(linesOf(generatedFile)).containsExactly(
				"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationConfiguration",
				"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationOnlyConfiguration");
	}

	@Test
	void notAnnotatedClasses() {
		assertThat(generateAnnotatedClasses(TestAutoConfigurationImportsAnnotationProcessor.class)).doesNotExist();
	}

	private File generateAnnotatedClasses(Class<?>... types) {
		TestAutoConfigurationImportsAnnotationProcessor processor = new TestAutoConfigurationImportsAnnotationProcessor();
		this.compiler.getTask(types).call(processor);
		return new File(this.tempDir, processor.getImportsFilePath());
	}

}
