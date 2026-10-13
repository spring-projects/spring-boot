/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.env;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.support.EnvironmentPostProcessorsFactory;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentPostProcessorsFactory}.
 *
 * @author Phillip Webb
 */
class EnvironmentPostProcessorsFactoryTests {

	private final DeferredLogFactory logFactory = Supplier::get;

	private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	@Test
	void fromSpringFactoriesReturnsFactory() {
		EnvironmentPostProcessorsFactory factory = EnvironmentPostProcessorsFactory.fromSpringFactories(null);
		List<EnvironmentPostProcessor> processors = factory.getEnvironmentPostProcessors(this.logFactory,
				this.bootstrapContext);
		assertThat(processors).hasSizeGreaterThan(1);
	}

	@Test
	void ofClassesReturnsFactory() {
		EnvironmentPostProcessorsFactory factory = EnvironmentPostProcessorsFactory
			.of(TestEnvironmentPostProcessor.class);
		List<EnvironmentPostProcessor> processors = factory.getEnvironmentPostProcessors(this.logFactory,
				this.bootstrapContext);
		assertThat(processors).hasSize(1);
		assertThat(processors.get(0)).isInstanceOf(TestEnvironmentPostProcessor.class);
	}

	@Test
	void ofClassNamesReturnsFactory() {
		EnvironmentPostProcessorsFactory factory = EnvironmentPostProcessorsFactory
			.of(TestEnvironmentPostProcessor.class.getName());
		List<EnvironmentPostProcessor> processors = factory.getEnvironmentPostProcessors(this.logFactory,
				this.bootstrapContext);
		assertThat(processors).hasSize(1);
		assertThat(processors.get(0)).isInstanceOf(TestEnvironmentPostProcessor.class);
	}

	@Test
	void ofClassNamesWithClassLoaderReturnsFactory() {
		OverridingClassLoader classLoader = new OverridingClassLoader(getClass().getClassLoader()) {

			@Override
			protected boolean isEligibleForOverriding(String className) {
				return super.isEligibleForOverriding(className)
						&& className.equals(TestEnvironmentPostProcessor.class.getName());
			}

		};
		EnvironmentPostProcessorsFactory factory = EnvironmentPostProcessorsFactory.of(classLoader,
				TestEnvironmentPostProcessor.class.getName());
		List<EnvironmentPostProcessor> processors = factory.getEnvironmentPostProcessors(this.logFactory,
				this.bootstrapContext);
		assertThat(processors).hasSize(1);
		assertThat(processors.get(0).getClass().getClassLoader()).isSameAs(classLoader);
	}

	static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

		TestEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		}

		@Override
		public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		}

	}

}
