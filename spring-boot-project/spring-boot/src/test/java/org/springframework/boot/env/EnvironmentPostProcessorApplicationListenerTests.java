/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aot.AotDetector;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.env.EnvironmentPostProcessorApplicationListener.EnvironmentBeanFactoryInitializationAotProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link EnvironmentPostProcessorApplicationListener}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class EnvironmentPostProcessorApplicationListenerTests {

	@Nested
	class ListenerTests {

		private final DeferredLogs deferredLogs = spy(new DeferredLogs());

		private final DefaultBootstrapContext bootstrapContext = spy(new DefaultBootstrapContext());

		private final EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();

		@BeforeEach
		void setup() {
			ReflectionTestUtils.setField(this.listener, "deferredLogs", this.deferredLogs);
			ReflectionTestUtils.setField(this.listener, "postProcessorsFactory",
					(Function<ClassLoader, EnvironmentPostProcessorsFactory>) (
							classLoader) -> EnvironmentPostProcessorsFactory.of(TestEnvironmentPostProcessor.class));
		}

		@Test
		void createUsesSpringFactories() {
			EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();
			assertThat(listener.getEnvironmentPostProcessors(null, this.bootstrapContext)).hasSizeGreaterThan(1);
		}

		@Test
		void createWhenHasFactoryUsesFactory() {
			EnvironmentPostProcessorApplicationListener listener = EnvironmentPostProcessorApplicationListener
				.with(EnvironmentPostProcessorsFactory.of(TestEnvironmentPostProcessor.class));
			List<EnvironmentPostProcessor> postProcessors = listener.getEnvironmentPostProcessors(null,
					this.bootstrapContext);
			assertThat(postProcessors).hasSize(1);
			assertThat(postProcessors.get(0)).isInstanceOf(TestEnvironmentPostProcessor.class);
		}

		@Test
		void supportsEventTypeWhenApplicationEnvironmentPreparedEventReturnsTrue() {
			assertThat(this.listener.supportsEventType(ApplicationEnvironmentPreparedEvent.class)).isTrue();
		}

		@Test
		void supportsEventTypeWhenApplicationPreparedEventReturnsTrue() {
			assertThat(this.listener.supportsEventType(ApplicationPreparedEvent.class)).isTrue();
		}

		@Test
		void supportsEventTypeWhenApplicationFailedEventReturnsTrue() {
			assertThat(this.listener.supportsEventType(ApplicationFailedEvent.class)).isTrue();
		}

		@Test
		void supportsEventTypeWhenOtherEventReturnsFalse() {
			assertThat(this.listener.supportsEventType(ApplicationStartingEvent.class)).isFalse();
		}

		@Test
		void onApplicationEventWhenApplicationEnvironmentPreparedEventCallsPostProcessors() {
			SpringApplication application = mock(SpringApplication.class);
			MockEnvironment environment = new MockEnvironment();
			ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(this.bootstrapContext,
					application, new String[0], environment);
			this.listener.onApplicationEvent(event);
			assertThat(environment.getProperty("processed")).isEqualTo("true");
		}

		@Test
		void onApplicationEventWhenApplicationPreparedEventSwitchesLogs() {
			SpringApplication application = mock(SpringApplication.class);
			ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
			ApplicationPreparedEvent event = new ApplicationPreparedEvent(application, new String[0], context);
			this.listener.onApplicationEvent(event);
			then(this.deferredLogs).should().switchOverAll();
		}

		@Test
		void onApplicationEventWhenApplicationFailedEventSwitchesLogs() {
			SpringApplication application = mock(SpringApplication.class);
			ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
			ApplicationFailedEvent event = new ApplicationFailedEvent(application, new String[0], context,
					new RuntimeException());
			this.listener.onApplicationEvent(event);
			then(this.deferredLogs).should().switchOverAll();
		}

		static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

			TestEnvironmentPostProcessor(DeferredLogFactory logFactory, BootstrapRegistry bootstrapRegistry) {
				assertThat(logFactory).isNotNull();
				assertThat(bootstrapRegistry).isNotNull();
			}

			@Override
			public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
				((MockEnvironment) environment).setProperty("processed", "true");
			}

		}

	}

	@Nested
	class AotTests {

		private static final ClassName TEST_APP = ClassName.get("com.example", "TestApp");

		@Test
		void aotContributionIsNotNecessaryWithDefaultConfiguration() {
			assertThat(getContribution(new StandardEnvironment())).isNull();
		}

		@Test
		void aotContributionIsNotNecessaryWithDefaultProfileActive() {
			StandardEnvironment environment = new StandardEnvironment();
			environment.setDefaultProfiles("fallback");
			environment.setActiveProfiles("fallback");
			assertThat(getContribution(environment)).isNull();
		}

		@Test
		void aotContributionRegistersActiveProfiles() {
			ConfigurableEnvironment environment = new StandardEnvironment();
			environment.setActiveProfiles("one", "two");
			compile(createContext(environment), (compiled) -> {
				EnvironmentPostProcessor environmentPostProcessor = compiled.getInstance(EnvironmentPostProcessor.class,
						ClassName.get("com.example", "TestApp__EnvironmentPostProcessor").toString());
				StandardEnvironment freshEnvironment = new StandardEnvironment();
				environmentPostProcessor.postProcessEnvironment(freshEnvironment, new SpringApplication());
				assertThat(freshEnvironment.getActiveProfiles()).containsExactly("one", "two");
			});
		}

		@Test
		void shouldUseAotEnvironmentPostProcessor() {
			SpringApplication application = new SpringApplication(ExampleAotProcessedApp.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setMainApplicationClass(ExampleAotProcessedApp.class);
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			try {
				ApplicationContext context = application.run();
				assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("one", "three");
				assertThat(context.getBean("test")).isEqualTo("test");
			}
			finally {
				System.clearProperty(AotDetector.AOT_ENABLED);
			}
		}

		@Test
		void aotEnvironmentPostProcessorShouldBeAppliedFirst(@TempDir Path tempDir) {
			Properties properties = new Properties();
			properties.put(EnvironmentPostProcessor.class.getName(), TestEnvironmentPostProcessor.class.getName());
			ClassLoader classLoader = createClassLoaderWithAdditionalSpringFactories(tempDir, properties);
			DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);

			SpringApplication application = new SpringApplication(ExampleAotProcessedApp.class);
			application.setResourceLoader(resourceLoader);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setMainApplicationClass(ExampleAotProcessedApp.class);
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			try {
				ApplicationContext context = application.run();
				// See TestEnvironmentPostProcessor
				assertThat(context.getEnvironment().getProperty("test.activeProfiles")).isEqualTo("one,three");
				assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("one", "three");
				assertThat(context.getBean("test")).isEqualTo("test");
			}
			finally {
				System.clearProperty(AotDetector.AOT_ENABLED);
			}
		}

		@Test
		void shouldBeLenientIfAotEnvironmentPostProcessorDoesNotExist() {
			SpringApplication application = new SpringApplication(ExampleAotProcessedNoProfileApp.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setMainApplicationClass(ExampleAotProcessedNoProfileApp.class);
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			try {
				ApplicationContext context = application.run();
				assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
				assertThat(context.getBean("test")).isEqualTo("test");
			}
			finally {
				System.clearProperty(AotDetector.AOT_ENABLED);
			}
		}

		private BeanFactoryInitializationAotContribution getContribution(ConfigurableEnvironment environment) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			beanFactory.registerSingleton(ConfigurableApplicationContext.ENVIRONMENT_BEAN_NAME, environment);
			return new EnvironmentBeanFactoryInitializationAotProcessor().processAheadOfTime(beanFactory);
		}

		private GenericApplicationContext createContext(ConfigurableEnvironment environment) {
			GenericApplicationContext context = new GenericApplicationContext();
			context.setEnvironment(environment);
			return context;
		}

		private void compile(GenericApplicationContext context, Consumer<Compiled> compiled) {
			TestGenerationContext generationContext = new TestGenerationContext(TEST_APP);
			new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
			generationContext.writeGeneratedContent();
			TestCompiler.forSystem().with(generationContext).compile(compiled);
		}

		private ClassLoader createClassLoaderWithAdditionalSpringFactories(Path tempDir, Properties properties) {
			return new ClassLoader() {
				@Override
				public Enumeration<URL> getResources(String name) throws IOException {
					Enumeration<URL> resources = super.getResources(name);
					if (SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION.equals(name)) {
						Path springFactories = tempDir.resolve("spring.factories");
						try (BufferedWriter writer = Files.newBufferedWriter(springFactories)) {
							properties.store(writer, "");
						}
						List<URL> allResources = new ArrayList<>();
						allResources.add(springFactories.toUri().toURL());
						allResources.addAll(Collections.list(resources));
						return Collections.enumeration(allResources);
					}
					return resources;
				}
			};
		}

		static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

			@Override
			public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
				MockPropertySource propertySource = new MockPropertySource().withProperty("test.activeProfiles",
						StringUtils.arrayToCommaDelimitedString(environment.getActiveProfiles()));
				environment.getPropertySources().addLast(propertySource);
			}

			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE;
			}

		}

		static class ExampleAotProcessedApp {

		}

		static class ExampleAotProcessedApp__ApplicationContextInitializer
				implements ApplicationContextInitializer<ConfigurableApplicationContext> {

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
				applicationContext.getBeanFactory().registerSingleton("test", "test");
			}

		}

		static class ExampleAotProcessedApp__EnvironmentPostProcessor implements EnvironmentPostProcessor {

			@Override
			public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
				environment.addActiveProfile("one");
				environment.addActiveProfile("three");
			}

		}

		static class ExampleAotProcessedNoProfileApp {

		}

		static class ExampleAotProcessedNoProfileApp__ApplicationContextInitializer
				implements ApplicationContextInitializer<ConfigurableApplicationContext> {

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
				applicationContext.getBeanFactory().registerSingleton("test", "test");
			}

		}

	}

}
