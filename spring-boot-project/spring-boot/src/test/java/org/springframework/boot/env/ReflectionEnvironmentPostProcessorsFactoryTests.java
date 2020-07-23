/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ReflectionEnvironmentPostProcessorsFactory}.
 *
 * @author Phillip Webb
 */
class ReflectionEnvironmentPostProcessorsFactoryTests {

	private final DeferredLogFactory logFactory = Supplier::get;

	@Test
	void createWithClassesCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestEnvironmentPostProcessor.class);
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void createWithClassNamesArrayCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void createWithClassNamesListCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				Arrays.asList(TestEnvironmentPostProcessor.class.getName()));
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasDefaultConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasLogFactoryConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestLogFactoryEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestLogFactoryEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasLogConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestLogEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestLogEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasNoSuitableConstructorThrowsException() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				BadEnvironmentPostProcessor.class.getName());
		assertThatIllegalArgumentException().isThrownBy(() -> factory.getEnvironmentPostProcessors(this.logFactory))
				.withMessageContaining("Unable to instantiate");
	}

	private EnvironmentPostProcessorsFactoryAssert assertThatFactory(EnvironmentPostProcessorsFactory factory) {
		return new EnvironmentPostProcessorsFactoryAssert(factory);
	}

	class EnvironmentPostProcessorsFactoryAssert {

		private EnvironmentPostProcessorsFactory factory;

		EnvironmentPostProcessorsFactoryAssert(EnvironmentPostProcessorsFactory factory) {
			this.factory = factory;
		}

		void createsSinglePostProcessor(Class<?> expectedType) {
			List<EnvironmentPostProcessor> processors = this.factory
					.getEnvironmentPostProcessors(ReflectionEnvironmentPostProcessorsFactoryTests.this.logFactory);
			assertThat(processors).hasSize(1);
			assertThat(processors.get(0)).isInstanceOf(expectedType);
		}

	}

	static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		}

	}

	static class TestLogFactoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

		TestLogFactoryEnvironmentPostProcessor(DeferredLogFactory logFactory) {
			assertThat(logFactory).isNotNull();
		}

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		}

	}

	static class TestLogEnvironmentPostProcessor implements EnvironmentPostProcessor {

		TestLogEnvironmentPostProcessor(Log log) {
			assertThat(log).isNotNull();
		}

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		}

	}

	static class BadEnvironmentPostProcessor implements EnvironmentPostProcessor {

		BadEnvironmentPostProcessor(InputStream inputStream) {
		}

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		}

	}

}
