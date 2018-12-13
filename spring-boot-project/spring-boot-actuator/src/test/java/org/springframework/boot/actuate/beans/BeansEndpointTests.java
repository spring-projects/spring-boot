/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.beans;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.beans.BeansEndpoint.ApplicationBeans;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.ContextBeans;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeansEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class BeansEndpointTests {

	@Test
	public void beansAreFound() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			ApplicationBeans result = context.getBean(BeansEndpoint.class).beans();
			ContextBeans descriptor = result.getContexts().get(context.getId());
			assertThat(descriptor.getParentId()).isNull();
			Map<String, BeanDescriptor> beans = descriptor.getBeans();
			assertThat(beans.size())
					.isLessThanOrEqualTo(context.getBeanDefinitionCount());
			assertThat(beans).containsKey("endpoint");
		});
	}

	@Test
	public void infrastructureBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) context
					.getAutowireCapableBeanFactory();
			List<String> infrastructureBeans = Stream.of(context.getBeanDefinitionNames())
					.filter((name) -> BeanDefinition.ROLE_INFRASTRUCTURE == factory
							.getBeanDefinition(name).getRole())
					.collect(Collectors.toList());
			ApplicationBeans result = context.getBean(BeansEndpoint.class).beans();
			ContextBeans contextDescriptor = result.getContexts().get(context.getId());
			Map<String, BeanDescriptor> beans = contextDescriptor.getBeans();
			for (String infrastructureBean : infrastructureBeans) {
				assertThat(beans).doesNotContainKey(infrastructureBean);
			}
		});
	}

	@Test
	public void lazyBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class,
						LazyBeanConfiguration.class);
		contextRunner.run((context) -> {
			ApplicationBeans result = context.getBean(BeansEndpoint.class).beans();
			ContextBeans contextDescriptor = result.getContexts().get(context.getId());
			assertThat(context).hasBean("lazyBean");
			assertThat(contextDescriptor.getBeans()).doesNotContainKey("lazyBean");
		});
	}

	@Test
	public void beansInParentContextAreFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
				.withUserConfiguration(BeanConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner()
					.withUserConfiguration(EndpointConfiguration.class).withParent(parent)
					.run((child) -> {
						ApplicationBeans result = child.getBean(BeansEndpoint.class)
								.beans();
						assertThat(result.getContexts().get(parent.getId()).getBeans())
								.containsKey("bean");
						assertThat(result.getContexts().get(child.getId()).getBeans())
								.containsKey("endpoint");
					});
		});
	}

	@Configuration
	public static class EndpointConfiguration {

		@Bean
		public BeansEndpoint endpoint(ConfigurableApplicationContext context) {
			return new BeansEndpoint(context);
		}

	}

	@Configuration
	static class BeanConfiguration {

		@Bean
		public String bean() {
			return "bean";
		}

	}

	@Configuration
	static class LazyBeanConfiguration {

		@Lazy
		@Bean
		public String lazyBean() {
			return "lazyBean";
		}

	}

}
