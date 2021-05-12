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

package org.springframework.boot.autoconfigure.validation;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfigurationTests.CustomValidatorConfiguration.TestBeanPostProcessor;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ValidationAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ValidationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner(
			AnnotationConfigApplicationContext::new)
					.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

	@Test
	void validationAutoConfigurationShouldConfigureDefaultValidator() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
					.containsExactly("defaultValidator");
			assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class)
					.isEqualTo(context.getBean(org.springframework.validation.Validator.class));
			assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
		});
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesValidatorShouldBackOff() {
		this.contextRunner.withUserConfiguration(UserDefinedValidatorConfig.class).run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("customValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
					.containsExactly("customValidator");
			assertThat(context.getBean(Validator.class)).isInstanceOf(OptionalValidatorFactoryBean.class)
					.isEqualTo(context.getBean(org.springframework.validation.Validator.class));
			assertThat(isPrimaryBean(context, "customValidator")).isFalse();
		});
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesDefaultValidatorShouldNotEnablePrimary() {
		this.contextRunner.withUserConfiguration(UserDefinedDefaultValidatorConfig.class).run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
					.containsExactly("defaultValidator");
			assertThat(isPrimaryBean(context, "defaultValidator")).isFalse();
		});
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesJsrValidatorShouldBackOff() {
		this.contextRunner.withUserConfiguration(UserDefinedJsrValidatorConfig.class).run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("customValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class)).isEmpty();
			assertThat(isPrimaryBean(context, "customValidator")).isFalse();
		});
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesSpringValidatorShouldCreateJsrValidator() {
		this.contextRunner.withUserConfiguration(UserDefinedSpringValidatorConfig.class).run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
					.containsExactly("customValidator", "anotherCustomValidator", "defaultValidator");
			assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class)
					.isEqualTo(context.getBean(org.springframework.validation.Validator.class));
			assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
		});
	}

	@Test
	void validationAutoConfigurationWhenUserProvidesPrimarySpringValidatorShouldRemovePrimaryFlag() {
		this.contextRunner.withUserConfiguration(UserDefinedPrimarySpringValidatorConfig.class).run((context) -> {
			assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
			assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
					.containsExactly("customValidator", "anotherCustomValidator", "defaultValidator");
			assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class);
			assertThat(context.getBean(org.springframework.validation.Validator.class))
					.isEqualTo(context.getBean("anotherCustomValidator"));
			assertThat(isPrimaryBean(context, "defaultValidator")).isFalse();
		});
	}

	@Test
	void whenUserProvidesSpringValidatorInParentContextThenAutoConfiguredValidatorIsPrimary() {
		new ApplicationContextRunner().withUserConfiguration(UserDefinedSpringValidatorConfig.class).run((parent) -> {
			this.contextRunner.withParent(parent).run((context) -> {
				assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
				assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
						.containsExactly("defaultValidator");
				assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context.getBeanFactory(),
						org.springframework.validation.Validator.class)).containsExactly("defaultValidator",
								"customValidator", "anotherCustomValidator");
				assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
			});
		});
	}

	@Test
	void whenUserProvidesPrimarySpringValidatorInParentContextThenAutoConfiguredValidatorIsPrimary() {
		new ApplicationContextRunner().withUserConfiguration(UserDefinedPrimarySpringValidatorConfig.class)
				.run((parent) -> {
					this.contextRunner.withParent(parent).run((context) -> {
						assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
						assertThat(context.getBeanNamesForType(org.springframework.validation.Validator.class))
								.containsExactly("defaultValidator");
						assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context.getBeanFactory(),
								org.springframework.validation.Validator.class)).containsExactly("defaultValidator",
										"customValidator", "anotherCustomValidator");
						assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
					});
				});
	}

	@Test
	void validationIsEnabled() {
		this.contextRunner.withUserConfiguration(SampleService.class).run((context) -> {
			assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
			SampleService service = context.getBean(SampleService.class);
			service.doSomething("Valid");
			assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething("KO"));
		});
	}

	@Test
	void classCanBeExcludedFromValidation() {
		this.contextRunner.withUserConfiguration(ExcludedServiceConfiguration.class).run((context) -> {
			assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
			ExcludedService service = context.getBean(ExcludedService.class);
			service.doSomething("Valid");
			assertThatNoException().isThrownBy(() -> service.doSomething("KO"));
		});
	}

	@Test
	void validationUsesCglibProxy() {
		this.contextRunner.withUserConfiguration(DefaultAnotherSampleService.class).run((context) -> {
			assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
			DefaultAnotherSampleService service = context.getBean(DefaultAnotherSampleService.class);
			service.doSomething(42);
			assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething(2));
		});
	}

	@Test
	void validationCanBeConfiguredToUseJdkProxy() {
		this.contextRunner.withUserConfiguration(AnotherSampleServiceConfiguration.class)
				.withPropertyValues("spring.aop.proxy-target-class=false").run((context) -> {
					assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
					assertThat(context.getBeansOfType(DefaultAnotherSampleService.class)).isEmpty();
					AnotherSampleService service = context.getBean(AnotherSampleService.class);
					service.doSomething(42);
					assertThatExceptionOfType(ConstraintViolationException.class)
							.isThrownBy(() -> service.doSomething(2));
				});
	}

	@Test
	void userDefinedMethodValidationPostProcessorTakesPrecedence() {
		this.contextRunner.withUserConfiguration(SampleConfiguration.class).run((context) -> {
			assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
			Object userMethodValidationPostProcessor = context.getBean("testMethodValidationPostProcessor");
			assertThat(context.getBean(MethodValidationPostProcessor.class))
					.isSameAs(userMethodValidationPostProcessor);
			assertThat(context.getBeansOfType(MethodValidationPostProcessor.class)).hasSize(1);
			assertThat(context.getBean(Validator.class))
					.isNotSameAs(ReflectionTestUtils.getField(userMethodValidationPostProcessor, "validator"));
		});
	}

	@Test
	void methodValidationPostProcessorValidatorDependencyDoesNotTriggerEarlyInitialization() {
		this.contextRunner.withUserConfiguration(CustomValidatorConfiguration.class)
				.run((context) -> assertThat(context.getBean(TestBeanPostProcessor.class).postProcessed)
						.contains("someService"));
	}

	private boolean isPrimaryBean(AssertableApplicationContext context, String beanName) {
		return ((BeanDefinitionRegistry) context.getSourceApplicationContext()).getBeanDefinition(beanName).isPrimary();
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

	@Configuration(proxyBeanMethods = false)
	static final class ExcludedServiceConfiguration {

		@Bean
		ExcludedService excludedService() {
			return new ExcludedService();
		}

		@Bean
		MethodValidationExcludeFilter exclusionFilter() {
			return (type) -> type.equals(ExcludedService.class);
		}

	}

	@Validated
	static final class ExcludedService {

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
