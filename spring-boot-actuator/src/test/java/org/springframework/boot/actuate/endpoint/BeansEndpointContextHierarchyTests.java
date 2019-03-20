/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeansEndpoint} with an application context hierarchy.
 *
 * @author Andy Wilkinson
 */
public class BeansEndpointContextHierarchyTests {

	private AnnotationConfigApplicationContext parent;

	private AnnotationConfigApplicationContext child;

	@After
	public void closeContexts() {
		if (this.child != null) {
			this.child.close();
		}
		if (this.parent != null) {
			this.parent.close();
		}
	}

	@Test
	public void beansInParentContextAreFound() {
		load(BeanConfiguration.class, EndpointConfiguration.class);
		BeansEndpoint endpoint = this.child.getBean(BeansEndpoint.class);
		List<Object> contexts = endpoint.invoke();
		assertThat(contexts).hasSize(2);
		assertThat(contexts.get(1)).has(beanNamed("bean"));
		assertThat(contexts.get(0)).has(beanNamed("endpoint"));
	}

	@Test
	public void beansInChildContextAreNotFound() {
		load(EndpointConfiguration.class, BeanConfiguration.class);
		BeansEndpoint endpoint = this.child.getBean(BeansEndpoint.class);
		List<Object> contexts = endpoint.invoke();
		assertThat(contexts).hasSize(1);
		assertThat(contexts.get(0)).has(beanNamed("endpoint"));
		assertThat(contexts.get(0)).doesNotHave(beanNamed("bean"));
	}

	private void load(Class<?> parent, Class<?> child) {
		this.parent = new AnnotationConfigApplicationContext(parent);
		this.child = new AnnotationConfigApplicationContext();
		this.child.setParent(this.parent);
		this.child.register(child);
		this.child.refresh();
	}

	private ContextHasBeanCondition beanNamed(String beanName) {
		return new ContextHasBeanCondition(beanName);
	}

	private static final class ContextHasBeanCondition extends Condition<Object> {

		private final String beanName;

		private ContextHasBeanCondition(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean matches(Object context) {
			if (!(context instanceof Map)) {
				return false;
			}
			List<Object> beans = (List<Object>) ((Map<String, Object>) context)
					.get("beans");
			if (beans == null) {
				return false;
			}
			for (Object bean : beans) {
				if (!(bean instanceof Map)) {
					return false;
				}
				if (this.beanName.equals(((Map<String, Object>) bean).get("bean"))) {
					return true;
				}
			}
			return false;
		}

	}

	@Configuration
	static class EndpointConfiguration {

		@Bean
		public BeansEndpoint endpoint() {
			return new BeansEndpoint();
		}

	}

	@Configuration
	static class BeanConfiguration {

		@Bean
		public String bean() {
			return "bean";
		}

	}

}
