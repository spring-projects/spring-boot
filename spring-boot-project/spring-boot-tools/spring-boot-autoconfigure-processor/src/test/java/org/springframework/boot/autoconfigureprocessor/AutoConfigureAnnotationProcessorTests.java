/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.testsupport.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureAnnotationProcessor}.
 *
 * @author Madhura Bhave
 */
public class AutoConfigureAnnotationProcessorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	@Test
	public void annotatedClass() throws Exception {
		Properties properties = compile(TestClassConfiguration.class);
		assertThat(properties).hasSize(3);
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor."
						+ "TestClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,org.springframework.boot.autoconfigureprocessor."
						+ "TestClassConfiguration$Nested");
		assertThat(properties).containsKey(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration");
		assertThat(properties).containsKey(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.Configuration");
		assertThat(properties).doesNotContainKey(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration$Nested");
	}

	@Test
	public void annotatedMethod() throws Exception {
		Properties properties = compile(TestMethodConfiguration.class);
		List<String> matching = new ArrayList<>();
		for (Object key : properties.keySet()) {
			if (key.toString().startsWith(
					"org.springframework.boot.autoconfigureprocessor.TestMethodConfiguration")) {
				matching.add(key.toString());
			}
		}
		assertThat(matching).hasSize(2)
				.contains("org.springframework.boot.autoconfigureprocessor."
						+ "TestMethodConfiguration")
				.contains("org.springframework.boot.autoconfigureprocessor."
						+ "TestMethodConfiguration.Configuration");
	}

	@Test
	public void annotatedClassWithOrder() throws Exception {
		Properties properties = compile(TestOrderedClassConfiguration.class);
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor."
						+ "TestOrderedClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,java.io.OutputStream");
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor."
						+ "TestOrderedClassConfiguration.AutoConfigureBefore",
				"test.before1,test.before2");
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor."
						+ "TestOrderedClassConfiguration.AutoConfigureAfter",
				"java.io.ObjectInputStream");
		assertThat(properties)
				.containsEntry(
						"org.springframework.boot.autoconfigureprocessor."
								+ "TestOrderedClassConfiguration.AutoConfigureOrder",
						"123");
	}

	private Properties compile(Class<?>... types) throws IOException {
		TestConditionMetadataAnnotationProcessor processor = new TestConditionMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getWrittenProperties();
	}

}
