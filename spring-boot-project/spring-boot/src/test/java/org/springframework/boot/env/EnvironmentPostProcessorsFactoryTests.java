/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
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
