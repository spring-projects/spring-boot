/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.validation;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfigurationTests.CustomValidatorConfiguration.TestBeanPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ValidationAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ValidationAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void validationAutoConfigurationShouldConfigureDefaultValidator() {
		load(Config.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("defaultValidator");
		Validator jsrValidator = this.context.getBean(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(isPrimaryBean("defaultValidator")).isTrue();
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesValidatorShouldBackOff() {
		load(UserDefinedValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("customValidator");
		assertThat(springValidatorNames).containsExactly("customValidator");
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		Validator jsrValidator = this.context.getBean(Validator.class);
		assertThat(jsrValidator).isInstanceOf(OptionalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(isPrimaryBean("customValidator")).isFalse();
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesDefaultValidatorShouldNotEnablePrimary() {
		load(UserDefinedDefaultValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("defaultValidator");
		assertThat(isPrimaryBean("defaultValidator")).isFalse();
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesJsrValidatorShouldBackOff() {
		load(UserDefinedJsrValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("customValidator");
		assertThat(springValidatorNames).isEmpty();
		assertThat(isPrimaryBean("customValidator")).isFalse();
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesSpringValidatorShouldCreateJsrValidator() {
		load(UserDefinedSpringValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("customValidator", "anotherCustomValidator",
				"defaultValidator");
		Validator jsrValidator = this.context.getBean(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(isPrimaryBean("defaultValidator")).isTrue();
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesPrimarySpringValidatorShouldRemovePrimaryFlag() {
		load(UserDefinedPrimarySpringValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("customValidator", "anotherCustomValidator",
				"defaultValidator");
		Validator jsrValidator = this.context.getBean(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(springValidator).isEqualTo(this.context.getBean("anotherCustomValidator"));
		assertThat(isPrimaryBean("defaultValidator")).isFalse();
	}

	@Test
	void validationIsEnabled() {
		load(SampleService.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		SampleService service = this.context.getBean(SampleService.class);
		service.doSomething("Valid");
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething("KO"));
	}

	@Test
	void validationUsesCglibProxy() {
		load(DefaultAnotherSampleService.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		DefaultAnotherSampleService service = this.context.getBean(DefaultAnotherSampleService.class);
		service.doSomething(42);
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething(2));
	}

	@Test
	void validationCanBeConfiguredToUseJdkProxy() {
		load(AnotherSampleServiceConfiguration.class, "spring.aop.proxy-target-class=false");
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(DefaultAnotherSampleService.class)).isEmpty();
		AnotherSampleService service = this.context.getBean(AnotherSampleService.class);
		service.doSomething(42);
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething(2));
	}

	@Test
	void userDefinedMethodValidationPostProcessorTakesPrecedence() {
		load(SampleConfiguration.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Object userMethodValidationPostProcessor = this.context.getBean("testMethodValidationPostProcessor");
		assertThat(this.context.getBean(MethodValidationPostProcessor.class))
				.isSameAs(userMethodValidationPostProcessor);
		assertThat(this.context.getBeansOfType(MethodValidationPostProcessor.class)).hasSize(1);
		assertThat(this.context.getBean(Validator.class))
				.isNotSameAs(ReflectionTestUtils.getField(userMethodValidationPostProcessor, "validator"));
	}

	@Test
	void methodValidationPostProcessorValidatorDependencyDoesNotTriggerEarlyInitialization() {
		load(CustomValidatorConfiguration.class);
		assertThat(this.context.getBean(TestBeanPostProcessor.class).postProcessed).contains("someService");
	}

	private boolean isPrimaryBean(String beanName) {
		return this.context.getBeanDefinition(beanName).isPrimary();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(ValidationAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDefinedValidatorConfig {

		@Bean
		OptionalValidatorFactoryBean customValidator() {
			return new OptionalValidatorFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDefinedDefaultValidatorConfig {

		@Bean
		OptionalValidatorFactoryBean defaultValidator() {
			return new OptionalValidatorFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDefinedJsrValidatorConfig {

		@Bean
		Validator customValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDefinedSpringValidatorConfig {

		@Bean
		org.springframework.validation.Validator customValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

		@Bean
		org.springframework.validation.Validator anotherCustomValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDefinedPrimarySpringValidatorConfig {

		@Bean
		org.springframework.validation.Validator customValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

		@Bean
		@Primary
		org.springframework.validation.Validator anotherCustomValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

	}

	@Validated
	static class SampleService {

		void doSomething(@Size(min = 3, max = 10) String name) {
		}

	}

	interface AnotherSampleService {

		void doSomething(@Min(42) Integer counter);

	}

	@Validated
	static class DefaultAnotherSampleService implements AnotherSampleService {

		@Override
		public void doSomething(Integer counter) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnotherSampleServiceConfiguration {

		@Bean
		AnotherSampleService anotherSampleService() {
			return new DefaultAnotherSampleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SampleConfiguration {

		@Bean
		MethodValidationPostProcessor testMethodValidationPostProcessor() {
			return new MethodValidationPostProcessor();
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class CustomValidatorConfiguration {

		CustomValidatorConfiguration(SomeService someService) {

		}

		@Bean
		Validator customValidator() {
			return new CustomValidatorBean();
		}

		@Bean
		static TestBeanPostProcessor testBeanPostProcessor() {
			return new TestBeanPostProcessor();
		}

		@Configuration(proxyBeanMethods = false)
		static class SomeServiceConfiguration {

			@Bean
			SomeService someService() {
				return new SomeService();
			}

		}

		static class SomeService {

		}

		static class TestBeanPostProcessor implements BeanPostProcessor {

			private Set<String> postProcessed = new HashSet<>();

			@Override
			public Object postProcessAfterInitialization(Object bean, String name) {
				this.postProcessed.add(name);
				return bean;
			}

			@Override
			public Object postProcessBeforeInitialization(Object bean, String name) {
				return bean;
			}

		}

	}

}
