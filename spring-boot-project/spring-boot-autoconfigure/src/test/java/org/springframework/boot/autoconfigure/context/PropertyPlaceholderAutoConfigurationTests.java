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

package org.springframework.boot.autoconfigure.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyPlaceholderAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class PropertyPlaceholderAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void whenTheAutoConfigurationIsNotUsedThenBeanDefinitionPlaceholdersAreNotResolved() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withInitializer(this::definePlaceholderBean)
			.run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("${fruit:apple}"));
	}

	@Test
	void whenTheAutoConfigurationIsUsedThenBeanDefinitionPlaceholdersAreResolved() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withInitializer(this::definePlaceholderBean)
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("banana"));
	}

	@Test
	void whenTheAutoConfigurationIsNotUsedThenValuePlaceholdersAreResolved() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withUserConfiguration(PlaceholderConfig.class)
			.run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("banana"));
	}

	@Test
	void whenTheAutoConfigurationIsUsedThenValuePlaceholdersAreResolved() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(PlaceholderConfig.class)
			.run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("banana"));
	}

	@Test
	void whenThereIsAUserDefinedPropertySourcesPlaceholderConfigurerThenItIsUsedForBeanDefinitionPlaceholderResolution() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withInitializer(this::definePlaceholderBean)
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(PlaceholdersOverride.class)
			.run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("orange"));
	}

	@Test
	void whenThereIsAUserDefinedPropertySourcesPlaceholderConfigurerThenItIsUsedForValuePlaceholderResolution() {
		this.contextRunner.withPropertyValues("fruit:banana")
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(PlaceholderConfig.class, PlaceholdersOverride.class)
			.run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("orange"));
	}

	private void definePlaceholderBean(ConfigurableApplicationContext context) {
		((BeanDefinitionRegistry) context.getBeanFactory()).registerBeanDefinition("placeholderBean",
				BeanDefinitionBuilder.rootBeanDefinition(PlaceholderBean.class)
					.addConstructorArgValue("${fruit:apple}")
					.getBeanDefinition());
	}

	@Configuration(proxyBeanMethods = false)
	static class PlaceholderConfig {

		@Value("${fruit:apple}")
		private String fruit;

	}

	static class PlaceholderBean {

		private final String fruit;

		PlaceholderBean(String fruit) {
			this.fruit = fruit;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PlaceholdersOverride {

		@Bean
		static PropertySourcesPlaceholderConfigurer morePlaceholders() {
			PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
			configurer
				.setProperties(StringUtils.splitArrayElementsIntoProperties(new String[] { "fruit=orange" }, "="));
			configurer.setLocalOverride(true);
			configurer.setOrder(0);
			return configurer;
		}

	}

}
