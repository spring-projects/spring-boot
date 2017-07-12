/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeansEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class BeansEndpointTests {

	@Test
	public void beansAreFound() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			List<Object> result = context.getBean(BeansEndpoint.class).beans();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isInstanceOf(Map.class);
		});
	}

	@Test
	public void beansInParentContextAreFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
				.withUserConfiguration(BeanConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner()
					.withUserConfiguration(EndpointConfiguration.class).withParent(parent)
					.run(child -> {
				BeansEndpoint endpoint = child.getBean(BeansEndpoint.class);
				List<Object> contexts = endpoint.beans();
				assertThat(contexts).hasSize(2);
				assertThat(contexts.get(1)).has(beanNamed("bean"));
				assertThat(contexts.get(0)).has(beanNamed("endpoint"));
			});
		});
	}

	@Test
	public void beansInChildContextAreNotFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(BeanConfiguration.class)
					.withParent(parent).run(child -> {
				BeansEndpoint endpoint = child.getBean(BeansEndpoint.class);
				List<Object> contexts = endpoint.beans();
				assertThat(contexts).hasSize(1);
				assertThat(contexts.get(0)).has(beanNamed("endpoint"));
				assertThat(contexts.get(0)).doesNotHave(beanNamed("bean"));
			});
		});
	}

	private ContextHasBeanCondition beanNamed(String beanName) {
		return new ContextHasBeanCondition(beanName);
	}

	private static final class ContextHasBeanCondition extends Condition<Object> {

		private final String beanName;

		private ContextHasBeanCondition(String beanName) {
			super("Bean named '" + beanName + "'");
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
	public static class EndpointConfiguration {

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
