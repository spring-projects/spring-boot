/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.util.Locale;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class BindFailureAnalyzerTests {

	@Before
	public void setup() {
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void cleanup() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	public void bindExceptionWithFieldErrorsDueToValidationFailure() {
		FailureAnalysis analysis = performAnalysis(
				FieldValidationFailureConfiguration.class);
		assertThat(analysis.getDescription())
				.contains(failure("test.foo.foo", "null", "may not be null"));
		assertThat(analysis.getDescription())
				.contains(failure("test.foo.value", "0", "at least five"));
		assertThat(analysis.getDescription())
				.contains(failure("test.foo.nested.bar", "null", "may not be null"));
	}

	@Test
	public void bindExceptionWithObjectErrorsDueToValidationFailure() throws Exception {
		FailureAnalysis analysis = performAnalysis(
				ObjectValidationFailureConfiguration.class);
		assertThat(analysis.getDescription())
				.contains("Reason: This object could not be bound.");
	}

	private static String failure(String property, String value, String reason) {
		return String.format("Property: %s%n    Value: %s%n    Reason: %s", property,
				value, reason);
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		BeanCreationException failure = createFailure(configuration);
		assertThat(failure).isNotNull();
		return new BindFailureAnalyzer().analyze(failure);
	}

	private BeanCreationException createFailure(Class<?> configuration) {
		try {
			new AnnotationConfigApplicationContext(configuration).close();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	@EnableConfigurationProperties(FieldValidationFailureProperties.class)
	static class FieldValidationFailureConfiguration {

	}

	@EnableConfigurationProperties(ObjectErrorFailureProperties.class)
	static class ObjectValidationFailureConfiguration {

	}

	@ConfigurationProperties("test.foo")
	@Validated
	static class FieldValidationFailureProperties {

		@NotNull
		private String foo;

		@Min(value = 5, message = "at least five")
		private int value;

		@Valid
		private Nested nested = new Nested();

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public int getValue() {
			return this.value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public Nested getNested() {
			return this.nested;
		}

		public void setNested(Nested nested) {
			this.nested = nested;
		}

		static class Nested {

			@NotNull
			private String bar;

			public String getBar() {
				return this.bar;
			}

			public void setBar(String bar) {
				this.bar = bar;
			}

		}

	}

	@ConfigurationProperties("foo.bar")
	static class ObjectErrorFailureProperties implements Validator {

		@Override
		public void validate(Object target, Errors errors) {
			errors.reject("my.objectError", "This object could not be bound.");
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

}
