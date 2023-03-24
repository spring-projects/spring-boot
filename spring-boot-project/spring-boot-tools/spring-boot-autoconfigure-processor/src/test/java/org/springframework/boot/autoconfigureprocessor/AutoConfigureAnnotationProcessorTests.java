/*
 * Copyright 2012-2023 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link AutoConfigureAnnotationProcessor}.
 *
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Scott Frederick
 */
class AutoConfigureAnnotationProcessorTests {

	@Test
	void annotatedClass() {
		compile(TestClassConfiguration.class, (properties) -> {
			assertThat(properties).hasSize(7);
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnClass",
					"java.io.InputStream,org.springframework.boot.autoconfigureprocessor."
							+ "TestClassConfiguration$Nested,org.springframework.foo");
			assertThat(properties)
				.containsKey("org.springframework.boot.autoconfigureprocessor.TestClassConfiguration");
			assertThat(properties)
				.containsKey("org.springframework.boot.autoconfigureprocessor.TestClassConfiguration$Nested");
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnBean",
					"java.io.OutputStream");
			assertThat(properties).containsEntry("org.springframework.boot.autoconfigureprocessor."
					+ "TestClassConfiguration.ConditionalOnSingleCandidate", "java.io.OutputStream");
			assertThat(properties).containsEntry("org.springframework.boot.autoconfigureprocessor."
					+ "TestClassConfiguration.ConditionalOnWebApplication", "SERVLET");
		});
	}

	@Test
	void annotatedClassWithOnlyAutoConfiguration() {
		compile(TestAutoConfigurationOnlyConfiguration.class, (properties) -> {
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationOnlyConfiguration", "");
			assertThat(properties).doesNotContainEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationOnlyConfiguration.AutoConfigureAfter",
					"");
			assertThat(properties).doesNotContainEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationOnlyConfiguration.AutoConfigureBefore",
					"");
		});
	}

	@Test
	void annotatedClassWithOnBeanThatHasName() {
		compile(TestOnBeanWithNameClassConfiguration.class, (properties) -> {
			assertThat(properties).hasSize(2);
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestOnBeanWithNameClassConfiguration.ConditionalOnBean",
					"");
		});
	}

	@Test
	void annotatedMethod() {
		process(TestMethodConfiguration.class, (properties) -> assertThat(properties).isNull());
	}

	@Test
	void annotatedClassWithOrder() {
		compile(TestOrderedClassConfiguration.class, (properties) -> {
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
		});

	}

	@Test
	void annotatedClassWithAutoConfiguration() {
		compile(TestAutoConfigurationConfiguration.class, (properties) -> {
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationConfiguration", "");
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationConfiguration.AutoConfigureBefore",
					"java.io.InputStream,test.before1,test.before2");
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationConfiguration.AutoConfigureAfter",
					"java.io.OutputStream,test.after1,test.after2");
		});
	}

	@Test
	void annotatedClassWithAutoConfigurationMerged() {
		compile(TestMergedAutoConfigurationConfiguration.class, (properties) -> {
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestMergedAutoConfigurationConfiguration", "");
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestMergedAutoConfigurationConfiguration.AutoConfigureBefore",
					"java.io.InputStream,test.before1,test.before2,java.io.ObjectInputStream,test.before3,test.before4");
			assertThat(properties).containsEntry(
					"org.springframework.boot.autoconfigureprocessor.TestMergedAutoConfigurationConfiguration.AutoConfigureAfter",
					"java.io.OutputStream,test.after1,test.after2,java.io.ObjectOutputStream,test.after3,test.after4");
		});
	}

	@Test // gh-19370
	void propertiesAreFullRepeatable() {
		process(TestOrderedClassConfiguration.class, (firstFile) -> {
			String first = getFileContents(firstFile);
			process(TestOrderedClassConfiguration.class, (secondFile) -> {
				String second = getFileContents(secondFile);
				assertThat(first).isEqualTo(second).doesNotContain("#");
			});
		});
	}

	private void compile(Class<?> type, Consumer<Properties> consumer) {
		process(type, (writtenFile) -> consumer.accept(getWrittenProperties(writtenFile)));
	}

	private void process(Class<?> type, Consumer<InputStream> consumer) {
		TestAutoConfigureAnnotationProcessor processor = new TestAutoConfigureAnnotationProcessor();
		SourceFile sourceFile = SourceFile.forTestClass(type);
		TestCompiler compiler = TestCompiler.forSystem().withProcessors(processor).withSources(sourceFile);
		compiler.compile((compiled) -> {
			InputStream propertiesFile = compiled.getClassLoader()
				.getResourceAsStream(AutoConfigureAnnotationProcessor.PROPERTIES_PATH);
			consumer.accept(propertiesFile);
		});
	}

	private Properties getWrittenProperties(InputStream inputStream) {
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
		catch (IOException ex) {
			fail("Error reading properties", ex);
		}
		return null;
	}

	private String getFileContents(InputStream inputStream) {
		try {
			return new String(inputStream.readAllBytes());
		}
		catch (IOException ex) {
			fail("Error reading contents of properties file", ex);
		}
		return null;
	}

}
