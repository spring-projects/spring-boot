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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LazyInitializationBeanFactoryPostProcessor}.
 *
 * @author Andy Wilkinson
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
