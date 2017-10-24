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

package org.springframework.boot.context.properties;

import java.time.Duration;

import javax.validation.constraints.NotNull;

import org.junit.Test;

import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBinderBuilder}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationPropertiesBinderBuilderTests {

	private final StaticApplicationContext applicationContext = new StaticApplicationContext();

	private final ConfigurationPropertiesBinderBuilder builder = new ConfigurationPropertiesBinderBuilder(
			this.applicationContext);

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	public void useCustomConversionService() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new AddressConverter());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"test.address=FooStreet 42");
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment)
				.withConversionService(conversionService).build();
		PropertyWithAddress target = new PropertyWithAddress();
		binder.bind(target);
		assertThat(target.getAddress()).isNotNull();
		assertThat(target.getAddress().streetName).isEqualTo("FooStreet");
		assertThat(target.getAddress().number).isEqualTo(42);
	}

	@Test
	public void detectDefaultConversionService() {
		this.applicationContext.registerSingleton("conversionService",
				DefaultConversionService.class);
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).build();
		assertThat(ReflectionTestUtils.getField(binder, "conversionService"))
				.isSameAs(this.applicationContext.getBean("conversionService"));
	}

	@Test
	public void bindToJavaTimeDuration() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"test.duration=PT1M");
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).build();
		PropertyWithDuration target = new PropertyWithDuration();
		binder.bind(target);
		assertThat(target.getDuration().getSeconds()).isEqualTo(60);
	}

	@Test
	public void useCustomValidator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).withValidator(validator).build();
		assertThat(ReflectionTestUtils.getField(binder, "validator")).isSameAs(validator);
	}

	@Test
	public void detectDefaultValidator() {
		this.applicationContext.registerSingleton("configurationPropertiesValidator",
				LocalValidatorFactoryBean.class);
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).build();
		assertThat(ReflectionTestUtils.getField(binder, "validator")).isSameAs(
				this.applicationContext.getBean("configurationPropertiesValidator"));
	}

	@Test
	public void validationWithoutJsr303() {
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).build();
		assertThat(bindWithValidationErrors(binder, new PropertyWithoutJSR303())
				.getAllErrors()).hasSize(1);
	}

	@Test
	public void validationWithJsr303() {
		ConfigurationPropertiesBinder binder = this.builder
				.withEnvironment(this.environment).build();
		assertThat(
				bindWithValidationErrors(binder, new PropertyWithJSR303()).getAllErrors())
						.hasSize(2);
	}

	@Test
	public void validationWithJsr303AndValidInput() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"test.foo=123456", "test.bar=654321");
		ConfigurationPropertiesBinder binder = new ConfigurationPropertiesBinder(
				this.environment.getPropertySources(), null, null);
		PropertyWithJSR303 target = new PropertyWithJSR303();
		binder.bind(target);
		assertThat(target.getFoo()).isEqualTo("123456");
		assertThat(target.getBar()).isEqualTo("654321");
	}

	private ValidationErrors bindWithValidationErrors(
			ConfigurationPropertiesBinder binder, Object target) {
		try {
			binder.bind(target);
			throw new AssertionError("Should have failed to bind " + target);
		}
		catch (ConfigurationPropertiesBindingException ex) {
			Throwable rootCause = ex.getRootCause();
			assertThat(rootCause).isInstanceOf(BindValidationException.class);
			return ((BindValidationException) rootCause).getValidationErrors();
		}
	}

	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithAddress {

		private Address address;

		public Address getAddress() {
			return this.address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

	}

	private static class Address {

		private String streetName;

		private Integer number;

		Address(String streetName, Integer number) {
			this.streetName = streetName;
			this.number = number;
		}

	}

	private static class AddressConverter implements Converter<String, Address> {
		@Override
		public Address convert(String source) {
			String[] split = StringUtils.split(source, " ");
			return new Address(split[0], Integer.valueOf(split[1]));
		}
	}

	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithDuration {

		private Duration duration;

		public Duration getDuration() {
			return this.duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	public static class PropertyWithoutJSR303 implements Validator {

		private String foo;

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.isAssignableFrom(getClass());
		}

		@Override
		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	public static class PropertyWithJSR303 extends PropertyWithoutJSR303 {

		@NotNull
		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

	}

}
