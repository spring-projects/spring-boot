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

package org.springframework.boot.sql.init.dependency;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DatabaseInitializationDependencyConfigurer}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DatabaseInitializationDependencyConfigurerTests {

	private final ConfigurableEnvironment environment = new MockEnvironment();

	@TempDir
	File temp;

	@BeforeEach
	void resetMocks() {
		reset(MockDatabaseInitializerDetector.instance, OrderedMockDatabaseInitializerDetector.instance,
				MockedDependsOnDatabaseInitializationDetector.instance);
	}

	@Test
	void beanFactoryPostProcessorHasOrderAllowingSubsequentPostProcessorsToFineTuneDependencies() {
		performDetection(Arrays.asList(MockDatabaseInitializerDetector.class,
				MockedDependsOnDatabaseInitializationDetector.class), (context) -> {
					BeanDefinition alpha = BeanDefinitionBuilder.genericBeanDefinition(String.class)
							.getBeanDefinition();
					BeanDefinition bravo = BeanDefinitionBuilder.genericBeanDefinition(String.class)
							.getBeanDefinition();
					context.register(DependsOnCaptor.class);
					context.register(DependencyConfigurerConfiguration.class);
					context.registerBeanDefinition("alpha", alpha);
					context.registerBeanDefinition("bravo", bravo);
					given(MockDatabaseInitializerDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("alpha"));
					given(MockedDependsOnDatabaseInitializationDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("bravo"));
					context.refresh();
					assertThat(DependsOnCaptor.dependsOn).hasEntrySatisfying("bravo",
							(dependencies) -> assertThat(dependencies).containsExactly("alpha"));
					assertThat(DependsOnCaptor.dependsOn).hasEntrySatisfying("alpha",
							(dependencies) -> assertThat(dependencies).isEmpty());
				});
	}

	@Test
	void whenDetectorsAreCreatedThenTheEnvironmentCanBeInjected() {
		performDetection(Arrays.asList(ConstructorInjectionDatabaseInitializerDetector.class,
				ConstructorInjectionDependsOnDatabaseInitializationDetector.class), (context) -> {
					BeanDefinition alpha = BeanDefinitionBuilder.genericBeanDefinition(String.class)
							.getBeanDefinition();
					context.registerBeanDefinition("alpha", alpha);
					context.register(DependencyConfigurerConfiguration.class);
					context.refresh();
					assertThat(ConstructorInjectionDatabaseInitializerDetector.environment).isEqualTo(this.environment);
					assertThat(ConstructorInjectionDependsOnDatabaseInitializationDetector.environment)
							.isEqualTo(this.environment);
				});
	}

	@Test
	void whenDependenciesAreConfiguredThenBeansThatDependUponDatabaseInitializationDependUponDetectedDatabaseInitializers() {
		BeanDefinition alpha = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		BeanDefinition bravo = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		performDetection(Arrays.asList(MockDatabaseInitializerDetector.class,
				MockedDependsOnDatabaseInitializationDetector.class), (context) -> {
					context.registerBeanDefinition("alpha", alpha);
					context.registerBeanDefinition("bravo", bravo);
					given(MockDatabaseInitializerDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("alpha"));
					given(MockedDependsOnDatabaseInitializationDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("bravo"));
					context.register(DependencyConfigurerConfiguration.class);
					context.refresh();
					assertThat(alpha.getAttribute(DatabaseInitializerDetector.class.getName()))
							.isEqualTo(MockDatabaseInitializerDetector.class.getName());
					assertThat(bravo.getAttribute(DatabaseInitializerDetector.class.getName())).isNull();
					verify(MockDatabaseInitializerDetector.instance).detectionComplete(context.getBeanFactory(),
							Collections.singleton("alpha"));
					assertThat(bravo.getDependsOn()).containsExactly("alpha");
				});
	}

	@Test
	void whenDependenciesAreConfiguredDetectedDatabaseInitializersAreInitializedInCorrectOrder() {
		BeanDefinition alpha = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		BeanDefinition bravo = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		BeanDefinition charlie = BeanDefinitionBuilder.genericBeanDefinition(String.class).getBeanDefinition();
		performDetection(Arrays.asList(MockDatabaseInitializerDetector.class,
				OrderedMockDatabaseInitializerDetector.class, MockedDependsOnDatabaseInitializationDetector.class),
				(context) -> {
					given(MockDatabaseInitializerDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("alpha"));
					given(OrderedMockDatabaseInitializerDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("bravo"));
					given(MockedDependsOnDatabaseInitializationDetector.instance.detect(context.getBeanFactory()))
							.willReturn(Collections.singleton("charlie"));
					context.registerBeanDefinition("alpha", alpha);
					context.registerBeanDefinition("bravo", bravo);
					context.registerBeanDefinition("charlie", charlie);
					context.register(DependencyConfigurerConfiguration.class);
					context.refresh();
					assertThat(charlie.getDependsOn()).containsExactly("alpha", "bravo");
					assertThat(bravo.getDependsOn()).containsExactly("alpha");
					assertThat(alpha.getDependsOn()).isNullOrEmpty();
				});
	}

	private void performDetection(Collection<Class<?>> detectors,
			Consumer<AnnotationConfigApplicationContext> contextCallback) {
		DetectorSpringFactoriesClassLoader detectorSpringFactories = new DetectorSpringFactoriesClassLoader(this.temp);
		detectors.forEach(detectorSpringFactories::register);
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(this.environment);
			context.setClassLoader(detectorSpringFactories);
			contextCallback.accept(context);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(DatabaseInitializationDependencyConfigurer.class)
	static class DependencyConfigurerConfiguration {

	}

	static class ConstructorInjectionDatabaseInitializerDetector implements DatabaseInitializerDetector {

		private static Environment environment;

		ConstructorInjectionDatabaseInitializerDetector(Environment environment) {
			ConstructorInjectionDatabaseInitializerDetector.environment = environment;
		}

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return Collections.singleton("alpha");
		}

	}

	static class ConstructorInjectionDependsOnDatabaseInitializationDetector
			implements DependsOnDatabaseInitializationDetector {

		private static Environment environment;

		ConstructorInjectionDependsOnDatabaseInitializationDetector(Environment environment) {
			ConstructorInjectionDependsOnDatabaseInitializationDetector.environment = environment;
		}

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return Collections.emptySet();
		}

	}

	static class MockDatabaseInitializerDetector implements DatabaseInitializerDetector {

		private static DatabaseInitializerDetector instance = mock(DatabaseInitializerDetector.class);

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return instance.detect(beanFactory);
		}

		@Override
		public void detectionComplete(ConfigurableListableBeanFactory beanFactory,
				Set<String> databaseInitializerNames) {
			instance.detectionComplete(beanFactory, databaseInitializerNames);
		}

	}

	static class OrderedMockDatabaseInitializerDetector implements DatabaseInitializerDetector {

		private static DatabaseInitializerDetector instance = mock(DatabaseInitializerDetector.class);

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return instance.detect(beanFactory);
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

	}

	static class MockedDependsOnDatabaseInitializationDetector implements DependsOnDatabaseInitializationDetector {

		private static DependsOnDatabaseInitializationDetector instance = mock(
				DependsOnDatabaseInitializationDetector.class);

		@Override
		public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
			return instance.detect(beanFactory);
		}

	}

	static class DetectorSpringFactoriesClassLoader extends ClassLoader {

		private final Set<Class<DatabaseInitializerDetector>> databaseInitializerDetectors = new HashSet<>();

		private final Set<Class<DependsOnDatabaseInitializationDetector>> dependsOnDatabaseInitializationDetectors = new HashSet<>();

		private final File temp;

		DetectorSpringFactoriesClassLoader(File temp) {
			this.temp = temp;
		}

		@SuppressWarnings("unchecked")
		void register(Class<?> detector) {
			if (DatabaseInitializerDetector.class.isAssignableFrom(detector)) {
				this.databaseInitializerDetectors.add((Class<DatabaseInitializerDetector>) detector);
			}
			else if (DependsOnDatabaseInitializationDetector.class.isAssignableFrom(detector)) {
				this.dependsOnDatabaseInitializationDetectors
						.add((Class<DependsOnDatabaseInitializationDetector>) detector);
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
			properties.put(DatabaseInitializerDetector.class.getName(), String.join(",",
					this.databaseInitializerDetectors.stream().map(Class::getName).collect(Collectors.toList())));
			properties.put(DependsOnDatabaseInitializationDetector.class.getName(),
					String.join(",", this.dependsOnDatabaseInitializationDetectors.stream().map(Class::getName)
							.collect(Collectors.toList())));
			File springFactories = new File(this.temp, "spring.factories");
			try (FileWriter writer = new FileWriter(springFactories)) {
				properties.store(writer, "");
			}
			return Collections.enumeration(Collections.singleton(springFactories.toURI().toURL()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DependsOnCaptor {

		static final Map<String, List<String>> dependsOn = new HashMap<>();

		@Bean
		static BeanFactoryPostProcessor dependsOnCapturingPostProcessor() {
			return (beanFactory) -> {
				dependsOn.clear();
				for (String name : beanFactory.getBeanDefinitionNames()) {
					storeDependsOn(name, beanFactory);
				}
			};
		}

		private static void storeDependsOn(String name, ConfigurableListableBeanFactory beanFactory) {
			String[] dependsOn = beanFactory.getBeanDefinition(name).getDependsOn();
			if (dependsOn != null) {
				DependsOnCaptor.dependsOn.put(name, Arrays.asList(dependsOn));
			}
		}

	}

}
