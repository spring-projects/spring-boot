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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.TestcontainersConfiguration;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link TestcontainersLifecycleApplicationContextInitializer},
 * {@link TestcontainersLifecycleBeanPostProcessor}, and
 * {@link TestcontainersLifecycleBeanFactoryPostProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TestcontainersLifecycleApplicationContextInitializerTests {

	@BeforeEach
	void setUp() {
		TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "false");
	}

	@Test
	void whenStartableBeanInvokesStartOnRefresh() {
		Startable container = mock(Startable.class);
		AnnotationConfigApplicationContext applicationContext = createApplicationContext(container);
		then(container).shouldHaveNoInteractions();
		applicationContext.refresh();
		then(container).should().start();
		applicationContext.close();
	}

	@Test
	void whenStartableBeanInvokesCloseOnShutdown() {
		Startable container = mock(Startable.class);
		AnnotationConfigApplicationContext applicationContext = createApplicationContext(container);
		applicationContext.refresh();
		then(container).should(never()).close();
		applicationContext.close();
		then(container).should(times(1)).close();
	}

	@Test
	void whenReusableContainerAndReuseEnabledBeanInvokesStartButNotClose() {
		TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "true");
		GenericContainer<?> container = mock(GenericContainer.class);
		given(container.isShouldBeReused()).willReturn(true);
		AnnotationConfigApplicationContext applicationContext = createApplicationContext(container);
		then(container).shouldHaveNoInteractions();
		applicationContext.refresh();
		then(container).should().start();
		applicationContext.close();
		then(container).should(never()).close();
	}

	@Test
	void whenReusableContainerButReuseNotEnabledBeanInvokesStartAndClose() {
		GenericContainer<?> container = mock(GenericContainer.class);
		given(container.isShouldBeReused()).willReturn(true);
		AnnotationConfigApplicationContext applicationContext = createApplicationContext(container);
		then(container).shouldHaveNoInteractions();
		applicationContext.refresh();
		then(container).should().start();
		applicationContext.close();
		then(container).should(times(1)).close();
	}

	@Test
	void whenReusableContainerAndReuseEnabledBeanFromConfigurationInvokesStartButNotClose() {
		TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "true");
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.register(ReusableContainerConfiguration.class);
		applicationContext.refresh();
		GenericContainer<?> container = applicationContext.getBean(GenericContainer.class);
		then(container).should().start();
		applicationContext.close();
		then(container).should(never()).close();
	}

	@Test
	void whenReusableContainerButReuseNotEnabledBeanFromConfigurationInvokesStartAndClose() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.register(ReusableContainerConfiguration.class);
		applicationContext.refresh();
		GenericContainer<?> container = applicationContext.getBean(GenericContainer.class);
		then(container).should().start();
		applicationContext.close();
		then(container).should(times(1)).close();
	}

	@Test
	void doesNotInitializeSameContextMoreThanOnce() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		int initialNumberOfPostProcessors = applicationContext.getBeanFactoryPostProcessors().size();
		for (int i = 0; i < 10; i++) {
			new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		}
		assertThat(applicationContext.getBeanFactoryPostProcessors()).hasSize(initialNumberOfPostProcessors + 1);
	}

	@Test
	void dealsWithBeanCurrentlyInCreationException() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.register(BeanCurrentlyInCreationExceptionConfiguration2.class,
				BeanCurrentlyInCreationExceptionConfiguration1.class);
		applicationContext.refresh();
	}

	@Test
	void doesNotStartContainersWhenAotProcessingIsInProgress() {
		GenericContainer<?> container = mock(GenericContainer.class);
		AnnotationConfigApplicationContext applicationContext = createApplicationContext(container);
		then(container).shouldHaveNoInteractions();
		withSystemProperty(AbstractAotProcessor.AOT_PROCESSING, "true",
				() -> applicationContext.refreshForAotProcessing(new RuntimeHints()));
		then(container).shouldHaveNoInteractions();
		applicationContext.close();
	}

	@Test
	void setupStartupBasedOnEnvironmentProperty() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.getEnvironment()
			.getPropertySources()
			.addLast(new MapPropertySource("test", Map.of("spring.testcontainers.beans.startup", "parallel")));
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		AbstractBeanFactory beanFactory = (AbstractBeanFactory) applicationContext.getBeanFactory();
		BeanPostProcessor beanPostProcessor = beanFactory.getBeanPostProcessors()
			.stream()
			.filter(TestcontainersLifecycleBeanPostProcessor.class::isInstance)
			.findFirst()
			.get();
		assertThat(beanPostProcessor).extracting("startup").isEqualTo(TestcontainersStartup.PARALLEL);
	}

	private void withSystemProperty(String name, String value, Runnable action) {
		String previousValue = System.getProperty(name);
		System.setProperty(name, value);
		try {
			action.run();
		}
		finally {
			if (previousValue == null) {
				System.clearProperty(name);
			}
			else {
				System.setProperty(name, previousValue);
			}
		}
	}

	private AnnotationConfigApplicationContext createApplicationContext(Startable container) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.registerBean("container", Startable.class, () -> container);
		return applicationContext;
	}

	private AnnotationConfigApplicationContext createApplicationContext(GenericContainer<?> container) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.registerBean("container", GenericContainer.class, () -> container);
		return applicationContext;
	}

	@Configuration
	static class ReusableContainerConfiguration {

		@Bean
		GenericContainer<?> container() {
			GenericContainer<?> container = mock(GenericContainer.class);
			given(container.isShouldBeReused()).willReturn(true);
			return container;
		}

	}

	@Configuration
	static class BeanCurrentlyInCreationExceptionConfiguration1 {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	@Configuration
	static class BeanCurrentlyInCreationExceptionConfiguration2 {

		BeanCurrentlyInCreationExceptionConfiguration2(TestBean testBean) {
		}

		@Bean
		GenericContainer<?> container(TestBean testBean) {
			GenericContainer<?> container = mock(GenericContainer.class);
			given(container.isShouldBeReused()).willReturn(true);
			return container;
		}

	}

	static class TestBean {

	}

}
