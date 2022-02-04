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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingBean @ConditionalOnMissingBean} with generic bean return type.
 *
 * @author Uladzislau Seuruk
 */
@SuppressWarnings("resource")
class ConditionalOnMissingBeanGenericReturnTypeTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void genericWhenTypeArgumentNotMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringTypeArgumentsConfig.class,
						GenericWithIntegerTypeArgumentsConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("genericStringExampleBean", "genericIntegerExampleBean")));
	}

	@Test
	void genericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, GenericWithStringTypeArgumentsConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	void genericWhenSubclassTypeArgumentNotMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, GenericWithIntegerTypeArgumentsConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "genericIntegerExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentWithValueMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringTypeArgumentsConfig.class,
						TypeArgumentsConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericStringExampleBean")));
	}

	@Test
	void genericWithValueWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, TypeArgumentsConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentNotMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithIntegerTypeArgumentsConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericIntegerExampleBean",
						"parameterizedContainerGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringTypeArgumentsConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericStringExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean")));
	}

	private Consumer<ConfigurableApplicationContext> exampleBeanRequirement(String... names) {
		return (context) -> {
			String[] beans = context.getBeanNamesForType(GenericExampleBean.class);
			String[] containers = context.getBeanNamesForType(TestParameterizedContainer.class);
			assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomConfig {

		@Bean
		CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringTypeArgumentsConfig {

		@Bean
		@ConditionalOnMissingBean
		GenericExampleBean<String> genericStringExampleBean() {
			return new GenericExampleBean<>("genericStringExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerTypeArgumentsConfig {

		@Bean
		@ConditionalOnMissingBean
		GenericExampleBean<Integer> genericIntegerExampleBean() {
			return new GenericExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithValueConfig {

		@Bean
		@ConditionalOnMissingBean(GenericExampleBean.class)
		GenericExampleBean<String> genericStringWithValueExampleBean() {
			return new GenericExampleBean<>("genericStringWithValueExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithParameterizedContainerConfig {

		@Bean
		@ConditionalOnMissingBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<GenericExampleBean<String>> parameterizedContainerGenericExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@TestAnnotation
	static class GenericExampleBean<T> {

		private final T value;

		GenericExampleBean(T value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(this.value);
		}

	}

	static class CustomExampleBean extends GenericExampleBean<String> {

		CustomExampleBean() {
			super("custom subclass");
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface TestAnnotation {

	}

}
