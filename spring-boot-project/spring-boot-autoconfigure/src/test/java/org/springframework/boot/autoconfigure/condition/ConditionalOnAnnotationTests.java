/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnAnnotation @ConditionalOnAnnotation}.
 *
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 */
class ConditionalOnAnnotationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	// Tests

	@Test
	void testAnnotationAtConfigurationWhenPresent() {
		this.contextRunner.withUserConfiguration(DefaultSpringBootConfiguration.class, MyAutoConfiguration.class,
				MyAppConfiguration.class).run((context) -> this.hasBean(context, "exampleBean1"));
	}

	@Test
	void testAnnotationAtBeanWhenPresent() {
		this.contextRunner.withUserConfiguration(DefaultSpringBootConfiguration.class, MyAutoConfiguration.class,
				MyAppConfiguration.class).run((context) -> this.hasBean(context, "exampleBean2"));
	}

	@Test
	void testAnnotationAtConfigurationWhenNotPresent() {
		this.contextRunner
				.withUserConfiguration(DefaultSpringBootConfiguration.class, MyAutoConfigurationNotPresent.class,
						MyAppConfigurationNotPresent.class)
				.run((context) -> assertThat(context).doesNotHaveBean("exampleBean3"));
	}

	@Test
	void testAnnotationAtBeanWhenNotPresent() {
		this.contextRunner
				.withUserConfiguration(DefaultSpringBootConfiguration.class, MyAutoConfigurationNotPresent.class,
						MyAppConfigurationNotPresent.class)
				.run((context) -> assertThat(context).doesNotHaveBean("exampleBean4"));
	}

	@Test
	void testMultipleAnnotationsWhenPresentForAND() {
		this.contextRunner.withUserConfiguration(MultipleAnnotationsSpringBootConfiguration.class,
				MultipleAnnotationsConfiguration.class).run((context) -> this.hasBean(context, "exampleBean5"));
	}

	@Test
	void testMultipleAnnotationsWhenPresentForOR() {
		this.contextRunner.withUserConfiguration(MultipleAnnotationsSpringBootConfiguration.class,
				MultipleAnnotationsConfiguration.class).run((context) -> this.hasBean(context, "exampleBean6"));
	}

	@Test
	void testMultipleAnnotationsWhenNotPresentForAND() {
		this.contextRunner
				.withUserConfiguration(MultipleAnnotationsSpringBootConfiguration.class,
						MultipleAnnotationsConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("exampleBean7"));
	}

	@Test
	void testMultipleAnnotationsWhenNotPresentForOR() {
		this.contextRunner.withUserConfiguration(MultipleAnnotationsSpringBootConfiguration.class,
				MultipleAnnotationsConfiguration.class).run((context) -> this.hasBean(context, "exampleBean8"));
	}

	private void hasBean(AssertableApplicationContext context, String beanName) {
		assertThat(context).hasBean(beanName);
		assertThat(context.getBean(beanName).toString().equals(beanName));
	}

	// Configurations

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface TestAnnotation {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface SampleAnnotation {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ExampleAnnotation {

	}

	@Configuration
	@TestAnnotation
	static class DefaultSpringBootConfiguration {

	}

	@Configuration
	@TestAnnotation
	@SampleAnnotation
	static class MultipleAnnotationsSpringBootConfiguration {

	}

	@Configuration
	@ConditionalOnAnnotation(TestAnnotation.class)
	static class MyAutoConfiguration {

		@Bean
		ExampleBean exampleBean1() {
			return new ExampleBean("exampleBean1");
		}

	}

	@Configuration
	static class MyAppConfiguration {

		@Bean
		@ConditionalOnAnnotation(TestAnnotation.class)
		ExampleBean exampleBean2() {
			return new ExampleBean("exampleBean2");
		}

	}

	@Configuration
	@ConditionalOnAnnotation(SampleAnnotation.class)
	static class MyAutoConfigurationNotPresent {

		@Bean
		ExampleBean exampleBean3() {
			return new ExampleBean("exampleBean3");
		}

	}

	// Annotations

	@Configuration
	static class MyAppConfigurationNotPresent {

		@Bean
		@ConditionalOnAnnotation(SampleAnnotation.class)
		ExampleBean exampleBean4() {
			return new ExampleBean("exampleBean4");
		}

	}

	@Configuration
	static class MultipleAnnotationsConfiguration {

		@Bean
		@ConditionalOnAnnotation(value = { TestAnnotation.class, SampleAnnotation.class },
				conditionType = ConditionalOnAnnotation.ConditionType.AND)
		ExampleBean exampleBean5() {
			return new ExampleBean("exampleBean5");
		}

		@Bean
		@ConditionalOnAnnotation(value = { TestAnnotation.class, SampleAnnotation.class },
				conditionType = ConditionalOnAnnotation.ConditionType.OR)
		ExampleBean exampleBean6() {
			return new ExampleBean("exampleBean6");
		}

		@Bean
		@ConditionalOnAnnotation(value = { TestAnnotation.class, ExampleAnnotation.class },
				conditionType = ConditionalOnAnnotation.ConditionType.AND)
		ExampleBean exampleBean7() {
			return new ExampleBean("exampleBean7");
		}

		@Bean
		@ConditionalOnAnnotation(value = { TestAnnotation.class, ExampleAnnotation.class },
				conditionType = ConditionalOnAnnotation.ConditionType.OR)
		ExampleBean exampleBean8() {
			return new ExampleBean("exampleBean8");
		}

	}

	static class ExampleBean {

		private final String value;

		ExampleBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

}
