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

package org.springframework.boot.autoconfigure.validation;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfigurationTests.CustomValidatorConfiguration.TestBeanPostProcessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ValidationAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class ValidationAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void validationAutoConfigurationShouldConfigureDefaultValidator() {
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
	public void validationAutoConfigurationWhenUserProvidesValidatorShouldBackOff() {
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
	public void validationAutoConfigurationWhenUserProvidesDefaultValidatorShouldNotEnablePrimary() {
		load(UserDefinedDefaultValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("defaultValidator");
		assertThat(isPrimaryBean("defaultValidator")).isFalse();
	}

	@Test
	public void validationAutoConfigurationWhenUserProvidesJsrValidatorShouldBackOff() {
		load(UserDefinedJsrValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("customValidator");
		assertThat(springValidatorNames).isEmpty();
		assertThat(isPrimaryBean("customValidator")).isFalse();
	}

	@Test
	public void validationAutoConfigurationWhenUserProvidesSpringValidatorShouldCreateJsrValidator() {
		load(UserDefinedSpringValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("customValidator",
				"anotherCustomValidator", "defaultValidator");
		Validator jsrValidator = this.context.getBean(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(isPrimaryBean("defaultValidator")).isTrue();
	}

	@Test
	public void validationAutoConfigurationWhenUserProvidesPrimarySpringValidatorShouldRemovePrimaryFlag() {
		load(UserDefinedPrimarySpringValidatorConfig.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("customValidator",
				"anotherCustomValidator", "defaultValidator");
		Validator jsrValidator = this.context.getBean(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(springValidator)
				.isEqualTo(this.context.getBean("anotherCustomValidator"));
		assertThat(isPrimaryBean("defaultValidator")).isFalse();
	}

	@Test
	public void validationIsEnabled() {
		load(SampleService.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		SampleService service = this.context.getBean(SampleService.class);
		service.doSomething("Valid");
		this.thrown.expect(ConstraintViolationException.class);
		service.doSomething("KO");
	}

	@Test
	public void validationUsesCglibProxy() {
		load(DefaultAnotherSampleService.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		DefaultAnotherSampleService service = this.context
				.getBean(DefaultAnotherSampleService.class);
		service.doSomething(42);
		this.thrown.expect(ConstraintViolationException.class);
		service.doSomething(2);
	}

	@Test
	public void validationCanBeConfiguredToUseJdkProxy() {
		load(AnotherSampleServiceConfiguration.class,
				"spring.aop.proxy-target-class=false");
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(DefaultAnotherSampleService.class))
				.isEmpty();
		AnotherSampleService service = this.context.getBean(AnotherSampleService.class);
		service.doSomething(42);
		this.thrown.expect(ConstraintViolationException.class);
		service.doSomething(2);
	}

	@Test
	public void userDefinedMethodValidationPostProcessorTakesPrecedence() {
		load(SampleConfiguration.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Object userMethodValidationPostProcessor = this.context
				.getBean("testMethodValidationPostProcessor");
		assertThat(this.context.getBean(MethodValidationPostProcessor.class))
				.isSameAs(userMethodValidationPostProcessor);
		assertThat(this.context.getBeansOfType(MethodValidationPostProcessor.class))
				.hasSize(1);
		assertThat(this.context.getBean(Validator.class))
				.isNotSameAs(new DirectFieldAccessor(userMethodValidationPostProcessor)
						.getPropertyValue("validator"));
	}

	@Test
	public void methodValidationPostProcessorValidatorDependencyDoesNotTriggerEarlyInitialization() {
		load(CustomValidatorConfiguration.class);
		assertThat(this.context.getBean(TestBeanPostProcessor.class).postProcessed)
				.contains("someService");
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

	@Configuration
	static class Config {

	}

	@Configuration
	static class UserDefinedValidatorConfig {

		@Bean
		public OptionalValidatorFactoryBean customValidator() {
			return new OptionalValidatorFactoryBean();
		}

	}

	@Configuration
	static class UserDefinedDefaultValidatorConfig {

		@Bean
		public OptionalValidatorFactoryBean defaultValidator() {
			return new OptionalValidatorFactoryBean();
		}

	}

	@Configuration
	static class UserDefinedJsrValidatorConfig {

		@Bean
		public Validator customValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class UserDefinedSpringValidatorConfig {

		@Bean
		public org.springframework.validation.Validator customValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

		@Bean
		public org.springframework.validation.Validator anotherCustomValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

	}

	@Configuration
	static class UserDefinedPrimarySpringValidatorConfig {

		@Bean
		public org.springframework.validation.Validator customValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

		@Bean
		@Primary
		public org.springframework.validation.Validator anotherCustomValidator() {
			return mock(org.springframework.validation.Validator.class);
		}

	}

	@Validated
	static class SampleService {

		public void doSomething(@Size(min = 3, max = 10) String name) {

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

	@Configuration
	static class AnotherSampleServiceConfiguration {

		@Bean
		public AnotherSampleService anotherSampleService() {
			return new DefaultAnotherSampleService();
		}

	}

	@Configuration
	static class SampleConfiguration {

		@Bean
		public MethodValidationPostProcessor testMethodValidationPostProcessor() {
			return new MethodValidationPostProcessor();
		}

	}

	@org.springframework.context.annotation.Configuration
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

		@Configuration
		static class SomeServiceConfiguration {

			@Bean
			public SomeService someService() {
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
