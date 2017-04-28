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

package org.springframework.boot.autoconfigure.validation;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ValidationAutoConfiguration}.
 *
 * @author Stephane Nicoll
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
	public void validationAutoConfigurationShouldConfigureJsrAndSpringValidator()
			throws Exception {
		load(Config.class);
		Validator jsrValidator = this.context.getBean(Validator.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(LocalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(jsrValidatorNames).containsExactly("defaultValidator");
		assertThat(springValidatorNames).containsExactly("defaultValidator");
	}

	@Test
	public void validationAutoConfigurationWhenUserProvidesValidatorShouldBackOff()
			throws Exception {
		load(UserDefinedValidatorConfig.class);
		Validator jsrValidator = this.context.getBean(Validator.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isInstanceOf(OptionalValidatorFactoryBean.class);
		assertThat(jsrValidator).isEqualTo(springValidator);
		assertThat(jsrValidatorNames).containsExactly("customValidator");
		assertThat(springValidatorNames).containsExactly("customValidator");
	}

	@Test
	public void validationAutoConfigurationWhenUserProvidesJsrOnlyShouldAdaptIt()
			throws Exception {
		load(UserDefinedJsrValidatorConfig.class);
		Validator jsrValidator = this.context.getBean(Validator.class);
		String[] jsrValidatorNames = this.context.getBeanNamesForType(Validator.class);
		org.springframework.validation.Validator springValidator = this.context
				.getBean(org.springframework.validation.Validator.class);
		String[] springValidatorNames = this.context
				.getBeanNamesForType(org.springframework.validation.Validator.class);
		assertThat(jsrValidator).isNotEqualTo(springValidator);
		assertThat(springValidator).isInstanceOf(DelegatingValidator.class);
		assertThat(jsrValidatorNames).containsExactly("customValidator");
		assertThat(springValidatorNames).containsExactly("jsr303ValidatorAdapter");
	}

	@Test
	public void validationAutoConfigurationShouldBeEnabled() {
		load(ClassWithConstraint.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		ClassWithConstraint service = this.context.getBean(ClassWithConstraint.class);
		service.call("Valid");
		this.thrown.expect(ConstraintViolationException.class);
		service.call("KO");
	}

	@Test
	public void validationAutoConfigurationShouldUseCglibProxy() {
		load(ImplementationOfInterfaceWithConstraint.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		ImplementationOfInterfaceWithConstraint service = this.context
				.getBean(ImplementationOfInterfaceWithConstraint.class);
		service.call(42);
		this.thrown.expect(ConstraintViolationException.class);
		service.call(2);
	}

	@Test
	public void validationAutoConfigurationWhenProxyTargetClassIsFalseShouldUseJdkProxy() {
		load(AnotherSampleServiceConfiguration.class,
				"spring.aop.proxy-target-class=false");
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		assertThat(this.context
				.getBeansOfType(ImplementationOfInterfaceWithConstraint.class)).isEmpty();
		InterfaceWithConstraint service = this.context
				.getBean(InterfaceWithConstraint.class);
		service.call(42);
		this.thrown.expect(ConstraintViolationException.class);
		service.call(2);
	}

	@Test
	public void validationAutoConfigurationWhenUserDefinesMethodValidationPostProcessorShouldBackOff() {
		load(UserDefinedMethodValidationConfig.class);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Object userMethodValidationPostProcessor = this.context
				.getBean("customMethodValidationPostProcessor");
		assertThat(this.context.getBean(MethodValidationPostProcessor.class))
				.isSameAs(userMethodValidationPostProcessor);
		assertThat(this.context.getBeansOfType(MethodValidationPostProcessor.class))
				.hasSize(1);
		assertThat(this.context.getBean(Validator.class))
				.isNotSameAs(new DirectFieldAccessor(userMethodValidationPostProcessor)
						.getPropertyValue("validator"));
	}

	public void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
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
	static class UserDefinedJsrValidatorConfig {

		@Bean
		public Validator customValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class UserDefinedMethodValidationConfig {

		@Bean
		public MethodValidationPostProcessor customMethodValidationPostProcessor() {
			return new MethodValidationPostProcessor();
		}

	}

	@Configuration
	static class AnotherSampleServiceConfiguration {

		@Bean
		public InterfaceWithConstraint implementationOfInterfaceWithConstraint() {
			return new ImplementationOfInterfaceWithConstraint();
		}

	}

	@Validated
	static class ClassWithConstraint {

		public void call(@Size(min = 3, max = 10) String name) {

		}

	}

	interface InterfaceWithConstraint {

		void call(@Min(42) Integer counter);
	}

	@Validated
	static class ImplementationOfInterfaceWithConstraint
			implements InterfaceWithConstraint {

		@Override
		public void call(Integer counter) {

		}
	}

}
