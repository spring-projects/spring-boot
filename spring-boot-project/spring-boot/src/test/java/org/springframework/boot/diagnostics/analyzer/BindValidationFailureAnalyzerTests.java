/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindValidationFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class BindValidationFailureAnalyzerTests {

	@BeforeEach
	void setup() {
		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	void cleanup() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void bindExceptionWithFieldErrorsDueToValidationFailure() {
		FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class);
		assertThat(analysis.getDescription()).contains(failure("test.foo.foo", "null", "must not be null"));
		assertThat(analysis.getDescription()).contains(failure("test.foo.value", "0", "at least five"));
		assertThat(analysis.getDescription()).contains(failure("test.foo.nested.bar", "null", "must not be null"));
	}

	@Test
	void bindExceptionWithOriginDueToValidationFailure() {
		FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class, "test.foo.value=4");
		assertThat(analysis.getDescription()).contains("Origin: \"test.foo.value\" from property source \"test\"");
	}

	@Test
	void bindExceptionWithObjectErrorsDueToValidationFailure() {
		FailureAnalysis analysis = performAnalysis(ObjectValidationFailureConfiguration.class);
		assertThat(analysis.getDescription()).contains("Reason: This object could not be bound.");
	}

	@Test
	void otherBindExceptionShouldReturnAnalysis() {
		BindException cause = new BindException(new FieldValidationFailureProperties(),
				"fieldValidationFailureProperties");
		cause.addError(new FieldError("test", "value", "must not be null"));
		BeanCreationException rootFailure = new BeanCreationException("bean creation failure", cause);
		FailureAnalysis analysis = new BindValidationFailureAnalyzer().analyze(rootFailure, rootFailure);
		assertThat(analysis.getDescription()).contains(failure("test.value", "null", "must not be null"));
	}

	private static String failure(String property, String value, String reason) {
		return String.format("Property: %s%n    Value: \"%s\"%n    Reason: %s", property, value, reason);
	}

	private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
		BeanCreationException failure = createFailure(configuration, environment);
		assertThat(failure).isNotNull();
		return new BindValidationFailureAnalyzer().analyze(failure);
	}

	private BeanCreationException createFailure(Class<?> configuration, String... environment) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			addEnvironment(context, environment);
			context.register(configuration);
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	private void addEnvironment(AnnotationConfigApplicationContext context, String[] environment) {
		MutablePropertySources sources = context.getEnvironment().getPropertySources();
		Map<String, Object> map = new HashMap<>();
		for (String pair : environment) {
			int index = pair.indexOf('=');
			String key = (index > 0) ? pair.substring(0, index) : pair;
			String value = (index > 0) ? pair.substring(index + 1) : "";
			map.put(key.trim(), value.trim());
		}
		sources.addFirst(new MapPropertySource("test", map));
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
		private FieldValidationFailureProperties.Nested nested = new FieldValidationFailureProperties.Nested();

		String getFoo() {
			return this.foo;
		}

		void setFoo(String foo) {
			this.foo = foo;
		}

		int getValue() {
			return this.value;
		}

		void setValue(int value) {
			this.value = value;
		}

		FieldValidationFailureProperties.Nested getNested() {
			return this.nested;
		}

		void setNested(FieldValidationFailureProperties.Nested nested) {
			this.nested = nested;
		}

		static class Nested {

			@NotNull
			private String bar;

			String getBar() {
				return this.bar;
			}

			void setBar(String bar) {
				this.bar = bar;
			}

		}

	}

	@ConfigurationProperties("foo.bar")
	@Validated
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
