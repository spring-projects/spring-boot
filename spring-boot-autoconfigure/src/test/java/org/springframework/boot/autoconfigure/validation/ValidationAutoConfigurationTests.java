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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

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

	public void load(Class<?> config) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		if (config != null) {
			applicationContext.register(config);
		}
		applicationContext.register(ValidationAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
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
	static class SampleConfiguration {

		@Bean
		public MethodValidationPostProcessor testMethodValidationPostProcessor() {
			return new MethodValidationPostProcessor();
		}

	}

}
