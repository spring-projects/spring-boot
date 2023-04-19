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

package org.springframework.boot.actuate.beans;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeansDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.ContextBeansDescriptor;
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
class BeansEndpointTests {

	@Test
	void beansAreFound() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			BeansDescriptor result = context.getBean(BeansEndpoint.class).beans();
			ContextBeansDescriptor descriptor = result.getContexts().get(context.getId());
			assertThat(descriptor.getParentId()).isNull();
			Map<String, BeanDescriptor> beans = descriptor.getBeans();
			assertThat(beans).hasSizeLessThanOrEqualTo(context.getBeanDefinitionCount());
			assertThat(beans).containsKey("endpoint");
		});
	}

	@Test
	void infrastructureBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) context
				.getAutowireCapableBeanFactory();
			List<String> infrastructureBeans = Stream.of(context.getBeanDefinitionNames())
				.filter((name) -> BeanDefinition.ROLE_INFRASTRUCTURE == factory.getBeanDefinition(name).getRole())
				.toList();
			BeansDescriptor result = context.getBean(BeansEndpoint.class).beans();
			ContextBeansDescriptor contextDescriptor = result.getContexts().get(context.getId());
			Map<String, BeanDescriptor> beans = contextDescriptor.getBeans();
			for (String infrastructureBean : infrastructureBeans) {
				assertThat(beans).doesNotContainKey(infrastructureBean);
			}
		});
	}

	@Test
	void lazyBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfiguration.class, LazyBeanConfiguration.class);
		contextRunner.run((context) -> {
			BeansDescriptor result = context.getBean(BeansEndpoint.class).beans();
			ContextBeansDescriptor contextDescriptor = result.getContexts().get(context.getId());
			assertThat(context).hasBean("lazyBean");
			assertThat(contextDescriptor.getBeans()).doesNotContainKey("lazyBean");
		});
	}

	@Test
	void beansInParentContextAreFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
			.withUserConfiguration(BeanConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(EndpointConfiguration.class)
				.withParent(parent)
				.run((child) -> {
					BeansDescriptor result = child.getBean(BeansEndpoint.class).beans();
					assertThat(result.getContexts().get(parent.getId()).getBeans()).containsKey("bean");
					assertThat(result.getContexts().get(child.getId()).getBeans()).containsKey("endpoint");
				});
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfiguration {

		@Bean
		BeansEndpoint endpoint(ConfigurableApplicationContext context) {
			return new BeansEndpoint(context);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BeanConfiguration {

		@Bean
		String bean() {
			return "bean";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LazyBeanConfiguration {

		@Lazy
		@Bean
		String lazyBean() {
			return "lazyBean";
		}

	}

}
