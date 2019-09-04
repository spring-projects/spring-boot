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

package org.springframework.boot.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ValidationBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ValidationBindHandlerTests {

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private ValidationBindHandler handler;

	private Binder binder;

	private LocalValidatorFactoryBean validator;

	@BeforeEach
	void setup() {
		this.binder = new Binder(this.sources);
		this.validator = new LocalValidatorFactoryBean();
		this.validator.afterPropertiesSet();
		this.handler = new ValidationBindHandler(this.validator);
	}

	@Test
	void bindShouldBindWithoutHandler() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		ExampleValidatedBean bean = this.binder.bind("foo", Bindable.of(ExampleValidatedBean.class)).get();
		assertThat(bean.getAge()).isEqualTo(4);
	}

	@Test
	void bindShouldFailWithHandler() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		assertThatExceptionOfType(BindException.class)
				.isThrownBy(() -> this.binder.bind("foo", Bindable.of(ExampleValidatedBean.class), this.handler))
				.withCauseInstanceOf(BindValidationException.class);
	}

	@Test
	void bindShouldValidateNestedProperties() {
		this.sources.add(new MockConfigurationPropertySource("foo.nested.age", 4));
		assertThatExceptionOfType(BindException.class)
				.isThrownBy(
						() -> this.binder.bind("foo", Bindable.of(ExampleValidatedWithNestedBean.class), this.handler))
				.withCauseInstanceOf(BindValidationException.class);
	}

	@Test
	void bindShouldFailWithAccessToOrigin() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4, "file"));
		BindValidationException cause = bindAndExpectValidationError(() -> this.binder
				.bind(ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedBean.class), this.handler));
		ObjectError objectError = cause.getValidationErrors().getAllErrors().get(0);
		assertThat(Origin.from(objectError).toString()).isEqualTo("file");
	}

	@Test
	void bindShouldFailWithAccessToBoundProperties() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.name", "baz");
		source.put("foo.nested.age", "4");
		source.put("faf.bar", "baz");
		this.sources.add(source);
		BindValidationException cause = bindAndExpectValidationError(() -> this.binder.bind(
				ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
		Set<ConfigurationProperty> boundProperties = cause.getValidationErrors().getBoundProperties();
		assertThat(boundProperties).extracting((p) -> p.getName().toString()).contains("foo.nested.age",
				"foo.nested.name");
	}

	@Test
	void bindShouldFailWithAccessToNameAndValue() {
		this.sources.add(new MockConfigurationPropertySource("foo.nested.age", "4"));
		BindValidationException cause = bindAndExpectValidationError(() -> this.binder.bind(
				ConfigurationPropertyName.of("foo"), Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
		assertThat(cause.getValidationErrors().getName().toString()).isEqualTo("foo");
		assertThat(cause.getMessage()).contains("nested.age");
		assertThat(cause.getMessage()).contains("rejected value [4]");
	}

	@Test
	void bindShouldFailIfExistingValueIsInvalid() {
		ExampleValidatedBean existingValue = new ExampleValidatedBean();
		BindValidationException cause = bindAndExpectValidationError(
				() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
						Bindable.of(ExampleValidatedBean.class).withExistingValue(existingValue), this.handler));
		FieldError fieldError = (FieldError) cause.getValidationErrors().getAllErrors().get(0);
		assertThat(fieldError.getField()).isEqualTo("age");
	}

	@Test
	void bindShouldValidateWithoutAnnotation() {
		ExampleNonValidatedBean existingValue = new ExampleNonValidatedBean();
		bindAndExpectValidationError(() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
				Bindable.of(ExampleNonValidatedBean.class).withExistingValue(existingValue), this.handler));
	}

	@Test
	void bindShouldNotValidateDepthGreaterThanZero() {
		// gh-12227
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "baz");
		this.sources.add(source);
		ExampleValidatedBeanWithGetterException existingValue = new ExampleValidatedBeanWithGetterException();
		this.binder.bind(ConfigurationPropertyName.of("foo"),
				Bindable.of(ExampleValidatedBeanWithGetterException.class).withExistingValue(existingValue),
				this.handler);
	}

	@Test
	void bindShouldNotValidateIfOtherHandlersInChainThrowError() {
		this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
		ExampleValidatedBean bean = new ExampleValidatedBean();
		assertThatExceptionOfType(BindException.class)
				.isThrownBy(() -> this.binder.bind("foo",
						Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
				.withCauseInstanceOf(ConverterNotFoundException.class);
	}

	@Test
	void bindShouldValidateIfOtherHandlersInChainIgnoreError() {
		TestHandler testHandler = new TestHandler(null);
		this.handler = new ValidationBindHandler(testHandler, this.validator);
		this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
		ExampleValidatedBean bean = new ExampleValidatedBean();
		assertThatExceptionOfType(BindException.class)
				.isThrownBy(() -> this.binder.bind("foo",
						Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
				.withCauseInstanceOf(BindValidationException.class);
	}

	@Test
	void bindShouldValidateIfOtherHandlersInChainReplaceErrorWithResult() {
		TestHandler testHandler = new TestHandler(new ExampleValidatedBeanSubclass());
		this.handler = new ValidationBindHandler(testHandler, this.validator);
		this.sources.add(new MockConfigurationPropertySource("foo", "hello"));
		this.sources.add(new MockConfigurationPropertySource("foo.age", "bad"));
		this.sources.add(new MockConfigurationPropertySource("foo.years", "99"));
		ExampleValidatedBean bean = new ExampleValidatedBean();
		assertThatExceptionOfType(BindException.class)
				.isThrownBy(() -> this.binder.bind("foo",
						Bindable.of(ExampleValidatedBean.class).withExistingValue(bean), this.handler))
				.withCauseInstanceOf(BindValidationException.class)
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageContaining("years"));
	}

	private BindValidationException bindAndExpectValidationError(Runnable action) {
		try {
			action.run();
		}
		catch (BindException ex) {
			return (BindValidationException) ex.getCause();
		}
		throw new IllegalStateException("Did not throw");
	}

	static class ExampleNonValidatedBean {

		@Min(5)
		private int age;

		int getAge() {
			return this.age;
		}

		void setAge(int age) {
			this.age = age;
		}

	}

	@Validated
	static class ExampleValidatedBean {

		@Min(5)
		private int age;

		int getAge() {
			return this.age;
		}

		void setAge(int age) {
			this.age = age;
		}

	}

	public static class ExampleValidatedBeanSubclass extends ExampleValidatedBean {

		@Min(100)
		private int years;

		ExampleValidatedBeanSubclass() {
			setAge(20);
		}

		public int getYears() {
			return this.years;
		}

		public void setYears(int years) {
			this.years = years;
		}

	}

	@Validated
	static class ExampleValidatedWithNestedBean {

		@Valid
		private ExampleNested nested = new ExampleNested();

		ExampleNested getNested() {
			return this.nested;
		}

		void setNested(ExampleNested nested) {
			this.nested = nested;
		}

	}

	static class ExampleNested {

		private String name;

		@Min(5)
		private int age;

		@NotNull
		private String address;

		String getName() {
			return this.name;
		}

		void setName(String name) {
			this.name = name;
		}

		int getAge() {
			return this.age;
		}

		void setAge(int age) {
			this.age = age;
		}

		String getAddress() {
			return this.address;
		}

		void setAddress(String address) {
			this.address = address;
		}

	}

	@Validated
	static class ExampleValidatedBeanWithGetterException {

		int getAge() {
			throw new RuntimeException();
		}

	}

	static class TestHandler extends AbstractBindHandler {

		private Object result;

		TestHandler(Object result) {
			this.result = result;
		}

		@Override
		public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
				Exception error) throws Exception {
			return this.result;
		}

	}

}
