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

package org.springframework.boot.actuate.beans;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.beans.BeansEndpoint.ApplicationContextDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
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
			ApplicationContextDescriptor result = context.getBean(BeansEndpoint.class)
					.beans();
			assertThat(result.getParent()).isNull();
			assertThat(result.getContextId()).isEqualTo(context.getId());
			Map<String, BeanDescriptor> beans = result.getBeans();
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
			ApplicationContextDescriptor result = context.getBean(BeansEndpoint.class)
					.beans();
			Map<String, BeanDescriptor> beans = result.getBeans();
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
			ApplicationContextDescriptor result = context.getBean(BeansEndpoint.class)
					.beans();
			assertThat(context).hasBean("lazyBean");
			assertThat(result.getBeans()).doesNotContainKey("lazyBean");
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
				ApplicationContextDescriptor result = endpoint.beans();
				assertThat(result.getParent().getBeans()).containsKey("bean");
				assertThat(result.getBeans()).containsKey("endpoint");
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
