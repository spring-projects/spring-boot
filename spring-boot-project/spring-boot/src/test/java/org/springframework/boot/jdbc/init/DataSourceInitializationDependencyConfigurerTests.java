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

package org.springframework.boot.jdbc.init;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceInitializationDependencyConfigurer}.
 *
 * @author Andy Wilkinson
 */
class DataSourceInitializationDependencyConfigurerTests {

	private final ConfigurableEnvironment environment = new MockEnvironment();

	DataSourceInitializerDetector dataSourceInitializerDetector = MockedDataSourceInitializerDetector.mock;

	DependsOnDataSourceInitializationDetector dependsOnDataSourceInitializationDetector = MockedDependsOnDataSourceInitializationDetector.mock;

	@TempDir
	File temp;

	@BeforeEach
	void resetMocks() {
		reset(MockedDataSourceInitializerDetector.mock, MockedDependsOnDataSourceInitializationDetector.mock);
	}

	@Test
	void whenDetectorsAreCreatedThenTheEnvironmentCanBeInjected() {
		performDetection(Arrays.asList(ConstructorInjectionDataSourceInitializerDetector.class,
				ConstructorInjectionDependsOnDataSourceInitializationDetector.class), (context) -> {
					context.refresh();
					assertThat(ConstructorInjectionDataSourceInitializerDetector.environment)
							.isEqualTo(this.environment);
					assertThat(ConstructorInjectionDependsOnDataSourceInitializationDetector.environment)
							.isEqualTo(this.environment);
				});
	}

	@Test
	void whenDependenciesAreConfiguredThenBeansThatDependUponDataSourceInitializationDependUponDetectedDataSourceInitializers() {
		BeanDefinition alpha = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		BeanDefinition bravo = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		performDetection(Arrays.asList(MockedDataSourceInitializerDetector.class,
				MockedDependsOnDataSourceInitializationDetector.class), (context) -> {
					context.registerBeanDefinition("alpha", alpha);
					context.registerBeanDefinition("bravo", bravo);
					given(this.dataSourceInitializerDetector.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("alpha"));
					given(this.dependsOnDataSourceInitializationDetector.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("bravo"));
					context.refresh();
					assertThat(alpha.getAttribute(DataSourceInitializerDetector.class.getName()))
							.isEqualTo(MockedDataSourceInitializerDetector.class.getName());
					assertThat(bravo.getAttribute(DataSourceInitializerDetector.class.getName())).isNull();
					verify(this.dataSourceInitializerDetector).detectionComplete(context.getBeanFactory(),
							Collections.singleton("alpha"));
					assertThat(bravo.getDependsOn()).containsExactly("alpha");
				});
	}

	private void performDetection(Collection<Class<?>> detectors,
			Consumer<AnnotationConfigApplicationContext> contextCallback) {
		DetectorSpringFactoriesClassLoader detectorSpringFactories = new DetectorSpringFactoriesClassLoader(this.temp);
		detectors.forEach(detectorSpringFactories::register);
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(this.environment);
			context.setClassLoader(detectorSpringFactories);
			context.register(DependencyConfigurerConfiguration.class);
			contextCallback.accept(context);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(DataSourceInitializationDependencyConfigurer.class)
	static class DependencyConfigurerConfiguration {

	}

	static class ConstructorInjectionDataSourceInitializerDetector implements DataSourceInitializerDetector {

		private static Environment environment;

		ConstructorInjectionDataSourceInitializerDetector(Environment environment) {
			ConstructorInjectionDataSourceInitializerDetector.environment = environment;
		}

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return Collections.emptySet();
		}

	}

	static class ConstructorInjectionDependsOnDataSourceInitializationDetector
			implements DependsOnDataSourceInitializationDetector {

		private static Environment environment;

		ConstructorInjectionDependsOnDataSourceInitializationDetector(Environment environment) {
			ConstructorInjectionDependsOnDataSourceInitializationDetector.environment = environment;
		}

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return Collections.emptySet();
		}

	}

	static class MockedDataSourceInitializerDetector implements DataSourceInitializerDetector {

		private static DataSourceInitializerDetector mock = Mockito.mock(DataSourceInitializerDetector.class);

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return MockedDataSourceInitializerDetector.mock.detect(beanFactory);
		}

		@Override
		public void detectionComplete(ConfigurableListableBeanFactory beanFactory,
				Set<String> dataSourceInitializerNames) {
			mock.detectionComplete(beanFactory, dataSourceInitializerNames);
		}

	}

	static class MockedDependsOnDataSourceInitializationDetector implements DependsOnDataSourceInitializationDetector {

		private static DependsOnDataSourceInitializationDetector mock = Mockito
				.mock(DependsOnDataSourceInitializationDetector.class);

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return MockedDependsOnDataSourceInitializationDetector.mock.detect(beanFactory);
		}

	}

	static class DetectorSpringFactoriesClassLoader extends ClassLoader {

		private final Set<Class<DataSourceInitializerDetector>> dataSourceInitializerDetectors = new HashSet<>();

		private final Set<Class<DependsOnDataSourceInitializationDetector>> dependsOnDataSourceInitializationDetectors = new HashSet<>();

		private final File temp;

		DetectorSpringFactoriesClassLoader(File temp) {
			this.temp = temp;
		}

		@SuppressWarnings("unchecked")
		void register(Class<?> detector) {
			if (DataSourceInitializerDetector.class.isAssignableFrom(detector)) {
				this.dataSourceInitializerDetectors.add((Class<DataSourceInitializerDetector>) detector);
			}
			else if (DependsOnDataSourceInitializationDetector.class.isAssignableFrom(detector)) {
				this.dependsOnDataSourceInitializationDetectors
						.add((Class<DependsOnDataSourceInitializationDetector>) detector);
			}
			else {
				throw new IllegalArgumentException("Unsupported detector type '" + detector.getName() + "'");
			}
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if (!"META-INF/spring.factories".equals(name)) {
				return super.findResources(name);
			}
			Properties properties = new Properties();
			properties.put(DataSourceInitializerDetector.class.getName(), String.join(",",
					this.dataSourceInitializerDetectors.stream().map(Class::getName).collect(Collectors.toList())));
			properties.put(DependsOnDataSourceInitializationDetector.class.getName(),
					String.join(",", this.dependsOnDataSourceInitializationDetectors.stream().map(Class::getName)
							.collect(Collectors.toList())));
			File springFactories = new File(this.temp, "spring.factories");
			try (FileWriter writer = new FileWriter(springFactories)) {
				properties.store(writer, "");
			}
			return Collections.enumeration(Collections.singleton(springFactories.toURI().toURL()));
		}

	}

}
