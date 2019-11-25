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

package org.springframework.boot.autoconfigureprocessor;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureAnnotationProcessor}.
 *
 * @author Madhura Bhave
 */
class AutoConfigureAnnotationProcessorTests {

	@TempDir
	File tempDir;

	private TestCompiler compiler;

	@BeforeEach
	void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.tempDir);
	}

	@Test
	void annotatedClass() throws Exception {
		Properties properties = compile(TestClassConfiguration.class);
		assertThat(properties).hasSize(5);
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,org.springframework.boot.autoconfigureprocessor."
						+ "TestClassConfiguration$Nested,org.springframework.foo");
		assertThat(properties).containsKey("org.springframework.boot.autoconfigureprocessor.TestClassConfiguration");
		assertThat(properties)
				.doesNotContainKey("org.springframework.boot.autoconfigureprocessor.TestClassConfiguration$Nested");
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnBean",
				"java.io.OutputStream");
		assertThat(properties).containsEntry("org.springframework.boot.autoconfigureprocessor."
				+ "TestClassConfiguration.ConditionalOnSingleCandidate", "java.io.OutputStream");
		assertThat(properties).containsEntry("org.springframework.boot.autoconfigureprocessor."
				+ "TestClassConfiguration.ConditionalOnWebApplication", "SERVLET");
	}

	@Test
	void annotatedClassWithOnBeanThatHasName() throws Exception {
		Properties properties = compile(TestOnBeanWithNameClassConfiguration.class);
		assertThat(properties).hasSize(2);
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOnBeanWithNameClassConfiguration.ConditionalOnBean",
				"");
	}

	@Test
	void annotatedMethod() throws Exception {
		Properties properties = compile(TestMethodConfiguration.class);
		assertThat(properties).isNull();
	}

	@Test
	void annotatedClassWithOrder() throws Exception {
		Properties properties = compile(TestOrderedClassConfiguration.class);
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,java.io.OutputStream");
		assertThat(properties).containsEntry("org.springframework.boot.autoconfigureprocessor."
				+ "TestOrderedClassConfiguration.AutoConfigureBefore", "test.before1,test.before2");
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.AutoConfigureAfter",
				"java.io.ObjectInputStream");
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.AutoConfigureOrder",
				"123");
	}

	private Properties compile(Class<?>... types) throws IOException {
		TestAutoConfigureAnnotationProcessor processor = new TestAutoConfigureAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getWrittenProperties();
	}

}
