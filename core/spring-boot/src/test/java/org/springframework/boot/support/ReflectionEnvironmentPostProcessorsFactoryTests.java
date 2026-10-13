/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.support;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.OverridingClassLoader;
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

	private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	@Test
	void createWithClassesCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(
				TestEnvironmentPostProcessor.class);
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void createWithClassNamesArrayCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				TestEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void createWithClassNamesListCreatesFactory() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				Arrays.asList(TestEnvironmentPostProcessor.class.getName()));
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void createWithClassNamesAndClassLoaderListCreatesFactory() {
		OverridingClassLoader classLoader = new OverridingClassLoader(getClass().getClassLoader()) {

			@Override
			protected boolean isEligibleForOverriding(String className) {
				return super.isEligibleForOverriding(className)
						&& className.equals(TestEnvironmentPostProcessor.class.getName());
			}

		};
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(classLoader,
				Arrays.asList(TestEnvironmentPostProcessor.class.getName()));
		assertThatFactory(factory).createsSinglePostProcessorWithClassLoader(classLoader);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasDefaultConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				TestEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasLogFactoryConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				TestLogFactoryEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestLogFactoryEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasLogConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				TestLogEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestLogEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasBootstrapRegistryConstructorCreatesPostProcessors() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				TestBootstrapRegistryEnvironmentPostProcessor.class.getName());
		assertThatFactory(factory).createsSinglePostProcessor(TestBootstrapRegistryEnvironmentPostProcessor.class);
	}

	@Test
	void getEnvironmentPostProcessorsWhenHasNoSuitableConstructorThrowsException() {
		ReflectionEnvironmentPostProcessorsFactory factory = new ReflectionEnvironmentPostProcessorsFactory(null,
				BadEnvironmentPostProcessor.class.getName());
		assertThatIllegalArgumentException()
			.isThrownBy(() -> factory.getEnvironmentPostProcessors(this.logFactory, this.bootstrapContext))
			.withMessageContaining("Unable to instantiate");
	}

	private EnvironmentPostProcessorsFactoryAssert assertThatFactory(EnvironmentPostProcessorsFactory factory) {
		return new EnvironmentPostProcessorsFactoryAssert(factory);
	}

	class EnvironmentPostProcessorsFactoryAssert {

		private final EnvironmentPostProcessorsFactory factory;

		EnvironmentPostProcessorsFactoryAssert(EnvironmentPostProcessorsFactory factory) {
			this.factory = factory;
		}

		void createsSinglePostProcessor(Class<?> expectedType) {
			EnvironmentPostProcessor processor = getSingleProcessor();
			assertThat(processor).isInstanceOf(expectedType);
		}

		void createsSinglePostProcessorWithClassLoader(OverridingClassLoader classLoader) {
			EnvironmentPostProcessor processor = getSingleProcessor();
			assertThat(processor.getClass().getClassLoader()).isSameAs(classLoader);
		}

		private EnvironmentPostProcessor getSingleProcessor() {
			List<EnvironmentPostProcessor> processors = this.factory.getEnvironmentPostProcessors(
					ReflectionEnvironmentPostProcessorsFactoryTests.this.logFactory,
					ReflectionEnvironmentPostProcessorsFactoryTests.this.bootstrapContext);
			assertThat(processors).hasSize(1);
			return processors.get(0);
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

	static class TestBootstrapRegistryEnvironmentPostProcessor implements EnvironmentPostProcessor {

		TestBootstrapRegistryEnvironmentPostProcessor(BootstrapRegistry bootstrapRegistry) {
			assertThat(bootstrapRegistry).isNotNull();
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
