/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LazyInitializationBeanFactoryPostProcessor}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class LazyInitializationBeanFactoryPostProcessorTests {

	@Test
	void whenLazyInitializationIsEnabledThenNormalBeansAreNotInitializedUntilRequired() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
			context.register(BeanState.class, ExampleBean.class);
			context.refresh();
			BeanState beanState = context.getBean(BeanState.class);
			assertThat(beanState.initializedBeans).isEmpty();
			context.getBean(ExampleBean.class);
			assertThat(beanState.initializedBeans).containsExactly(ExampleBean.class);
		}
	}

	@Test
	void whenLazyInitializationIsEnabledThenSmartInitializingSingletonsAreInitializedDuringRefresh() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
			context.register(BeanState.class, ExampleSmartInitializingSingleton.class);
			context.refresh();
			BeanState beanState = context.getBean(BeanState.class);
			assertThat(beanState.initializedBeans).containsExactly(ExampleSmartInitializingSingleton.class);
			assertThat(context.getBean(ExampleSmartInitializingSingleton.class).callbackInvoked).isTrue();
		}
	}

	@Test
	void whenLazyInitializationIsEnabledThenInfrastructureRoleBeansAreInitializedDuringRefresh() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
			context.register(BeanState.class);
			context.registerBean(ExampleBean.class,
					(definition) -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
			context.refresh();
			BeanState beanState = context.getBean(BeanState.class);
			assertThat(beanState.initializedBeans).containsExactly(ExampleBean.class);
		}
	}

	static class ExampleBean {

		ExampleBean(BeanState beanState) {
			beanState.initializedBeans.add(getClass());
		}

	}

	static class ExampleSmartInitializingSingleton implements SmartInitializingSingleton {

		private boolean callbackInvoked;

		ExampleSmartInitializingSingleton(BeanState beanState) {
			beanState.initializedBeans.add(getClass());
		}

		@Override
		public void afterSingletonsInstantiated() {
			this.callbackInvoked = true;
		}

	}

	static class BeanState {

		private final List<Class<?>> initializedBeans = new ArrayList<>();

	}

}
