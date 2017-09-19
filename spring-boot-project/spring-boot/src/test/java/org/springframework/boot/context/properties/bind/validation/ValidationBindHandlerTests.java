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

package org.springframework.boot.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for {@link ValidationBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ValidationBindHandlerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private ValidationBindHandler handler;

	private Binder binder;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		this.handler = new ValidationBindHandler(validator);
	}

	@Test
	public void bindShouldBindWithoutHandler() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		ExampleValidatedBean bean = this.binder
				.bind("foo", Bindable.of(ExampleValidatedBean.class)).get();
		assertThat(bean.getAge()).isEqualTo(4);
	}

	@Test
	public void bindShouldFailWithHandler() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4));
		this.thrown.expect(BindException.class);
		this.thrown.expectCause(instanceOf(BindValidationException.class));
		this.binder.bind("foo", Bindable.of(ExampleValidatedBean.class), this.handler);
	}

	@Test
	public void bindShouldValidateNestedProperties() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.nested.age", 4));
		this.thrown.expect(BindException.class);
		this.thrown.expectCause(instanceOf(BindValidationException.class));
		this.binder.bind("foo", Bindable.of(ExampleValidatedWithNestedBean.class),
				this.handler);
	}

	@Test
	public void bindShouldFailWithAccessToOrigin() {
		this.sources.add(new MockConfigurationPropertySource("foo.age", 4, "file"));
		BindValidationException cause = bindAndExpectValidationError(
				() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
						Bindable.of(ExampleValidatedBean.class), this.handler));
		ObjectError objectError = cause.getValidationErrors().getAllErrors().get(0);
		assertThat(Origin.from(objectError).toString()).isEqualTo("file");
	}

	@Test
	public void bindShouldFailWithAccessToBoundProperties() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.name", "baz");
		source.put("foo.nested.age", "4");
		source.put("faf.bar", "baz");
		this.sources.add(source);
		BindValidationException cause = bindAndExpectValidationError(
				() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
						Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
		Set<ConfigurationProperty> boundProperties = cause.getValidationErrors()
				.getBoundProperties();
		assertThat(boundProperties).extracting((p) -> p.getName().toString())
				.contains("foo.nested.age", "foo.nested.name");
	}

	@Test
	public void bindShouldFailWithAccessToName() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.nested.age", "4"));
		BindValidationException cause = bindAndExpectValidationError(
				() -> this.binder.bind(ConfigurationPropertyName.of("foo"),
						Bindable.of(ExampleValidatedWithNestedBean.class), this.handler));
		assertThat(cause.getValidationErrors().getName().toString())
				.isEqualTo("foo.nested");
	}

	@Test
	public void bindShouldFailIfExistingValueIsInvalid() throws Exception {
		ExampleValidatedBean existingValue = new ExampleValidatedBean();
		BindValidationException cause = bindAndExpectValidationError(
				() -> this.binder.bind(ConfigurationPropertyName.of("foo"), Bindable
						.of(ExampleValidatedBean.class).withExistingValue(existingValue),
						this.handler));
		FieldError fieldError = (FieldError) cause.getValidationErrors().getAllErrors()
				.get(0);
		assertThat(fieldError.getField()).isEqualTo("age");
	}

	@Test
	public void bindShouldNotValidateWithoutAnnotation() throws Exception {
		ExampleNonValidatedBean existingValue = new ExampleNonValidatedBean();
		this.binder.bind(ConfigurationPropertyName.of("foo"), Bindable
				.of(ExampleNonValidatedBean.class).withExistingValue(existingValue),
				this.handler);
	}

	private BindValidationException bindAndExpectValidationError(Runnable action) {
		try {
			action.run();
		}
		catch (BindException ex) {
			ex.printStackTrace();

			BindValidationException cause = (BindValidationException) ex.getCause();
			return cause;
		}
		throw new IllegalStateException("Did not throw");
	}

	public static class ExampleNonValidatedBean {

		@Min(5)
		private int age;

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}

	}

	@Validated
	public static class ExampleValidatedBean {

		@Min(5)
		private int age;

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}

	}

	@Validated
	public static class ExampleValidatedWithNestedBean {

		@Valid
		private ExampleNested nested = new ExampleNested();

		public ExampleNested getNested() {
			return this.nested;
		}

		public void setNested(ExampleNested nested) {
			this.nested = nested;
		}

	}

	public static class ExampleNested {

		private String name;

		@Min(5)
		private int age;

		@NotNull
		private String address;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getAddress() {
			return this.address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}

}
