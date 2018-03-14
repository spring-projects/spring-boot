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

import java.util.HashMap;

import javax.validation.constraints.Min;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ValidatorAdapter}.
 *
 * @author Stephane Nicoll
 */
public class ValidatorAdapterTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void wrapLocalValidatorFactoryBean() {
		ValidatorAdapter wrapper = load(LocalValidatorFactoryBeanConfig.class);
		assertThat(wrapper.supports(SampleData.class)).isTrue();
		MapBindingResult errors = new MapBindingResult(new HashMap<String, Object>(),
				"test");
		wrapper.validate(new SampleData(40), errors);
		assertThat(errors.getErrorCount()).isEqualTo(1);
	}

	@Test
	public void wrapperInvokesCallbackOnNonManagedBean() {
		load(NonManagedBeanConfig.class);
		LocalValidatorFactoryBean validator = this.context
				.getBean(NonManagedBeanConfig.class).validator;
		verify(validator, times(1)).setApplicationContext(any(ApplicationContext.class));
		verify(validator, times(1)).afterPropertiesSet();
		verify(validator, never()).destroy();
		this.context.close();
		this.context = null;
		verify(validator, times(1)).destroy();
	}

	@Test
	public void wrapperDoesNotInvokeCallbackOnManagedBean() {
		load(ManagedBeanConfig.class);
		LocalValidatorFactoryBean validator = this.context
				.getBean(ManagedBeanConfig.class).validator;
		verify(validator, never()).setApplicationContext(any(ApplicationContext.class));
		verify(validator, never()).afterPropertiesSet();
		verify(validator, never()).destroy();
		this.context.close();
		this.context = null;
		verify(validator, never()).destroy();
	}

	private ValidatorAdapter load(Class<?> config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(ValidatorAdapter.class);
	}

	@Configuration
	static class LocalValidatorFactoryBeanConfig {

		@Bean
		public LocalValidatorFactoryBean validator() {
			return new LocalValidatorFactoryBean();
		}

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(validator(), true);
		}

	}

	@Configuration
	static class NonManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(
				LocalValidatorFactoryBean.class);

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, false);
		}

	}

	@Configuration
	static class ManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(
				LocalValidatorFactoryBean.class);

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, true);
		}

	}

	static class SampleData {

		@Min(42)
		private int counter;

		SampleData(int counter) {
			this.counter = counter;
		}

	}

}
