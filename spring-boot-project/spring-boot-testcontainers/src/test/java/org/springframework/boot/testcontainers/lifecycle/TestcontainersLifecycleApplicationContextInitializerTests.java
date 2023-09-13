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

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 */
class TestcontainersLifecycleApplicationContextInitializerTests {

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
	void whenReusableContainerBeanInvokesStartButNotClose() {
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
	void whenReusableContainerBeanFromConfigurationInvokesStartButNotClose() {
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

	private AnnotationConfigApplicationContext createApplicationContext(Startable container) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
		applicationContext.registerBean("container", Startable.class, () -> container);
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
