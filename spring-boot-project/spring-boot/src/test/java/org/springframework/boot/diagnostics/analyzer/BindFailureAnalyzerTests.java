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
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class BindFailureAnalyzerTests {

	@Test
	void analysisForUnboundElementsIsNull() {
		FailureAnalysis analysis = performAnalysis(UnboundElementsFailureConfiguration.class,
				"test.foo.listValue[0]=hello", "test.foo.listValue[2]=world");
		assertThat(analysis).isNull();
	}

	@Test
	void analysisForValidationExceptionIsNull() {
		FailureAnalysis analysis = performAnalysis(FieldValidationFailureConfiguration.class, "test.foo.value=1");
		assertThat(analysis).isNull();
	}

	@Test
	void bindExceptionDueToOtherFailure() {
		FailureAnalysis analysis = performAnalysis(GenericFailureConfiguration.class, "test.foo.value=alpha");
		assertThat(analysis.getDescription()).contains(failure("test.foo.value", "alpha",
				"\"test.foo.value\" from property source \"test\"", "failed to convert java.lang.String to int"));
	}

	@Test
	void bindExceptionForUnknownValueInEnumListsValidValuesInAction() {
		FailureAnalysis analysis = performAnalysis(EnumFailureConfiguration.class, "test.foo.fruit=apple,strawberry");
		for (Fruit fruit : Fruit.values()) {
			assertThat(analysis.getAction()).contains(fruit.name());
		}
	}

	@Test
	void bindExceptionWithNestedFailureShouldDisplayNestedMessage() {
		FailureAnalysis analysis = performAnalysis(NestedFailureConfiguration.class, "test.foo.value=hello");
		assertThat(analysis.getDescription()).contains(failure("test.foo.value", "hello",
				"\"test.foo.value\" from property source \"test\"", "java.lang.RuntimeException: This is a failure"));
	}

	@Test // gh-27028
	void bindExceptionDueToClassNotFoundConversionFailure() {
		FailureAnalysis analysis = performAnalysis(GenericFailureConfiguration.class,
				"test.foo.type=com.example.Missing");
		assertThat(analysis.getDescription()).contains(failure("test.foo.type", "com.example.Missing",
				"\"test.foo.type\" from property source \"test\"",
				"failed to convert java.lang.String to java.lang.Class<?> (caused by java.lang.ClassNotFoundException: com.example.Missing"));
	}

	@Test
	void bindExceptionDueToMapConversionFailure() {
		FailureAnalysis analysis = performAnalysis(LoggingLevelFailureConfiguration.class, "logging.level=debug");
		assertThat(analysis.getDescription()).contains(failure("logging.level", "debug",
				"\"logging.level\" from property source \"test\"",
				"org.springframework.core.convert.ConverterNotFoundException: No converter found capable of converting "
						+ "from type [java.lang.String] to type [java.util.Map<java.lang.String, "
						+ "org.springframework.boot.logging.LogLevel>]"));
	}

	private static String failure(String property, String value, String origin, String reason) {
		return String.format("Property: %s%n    Value: \"%s\"%n    Origin: %s%n    Reason: %s", property, value, origin,
				reason);
	}

	private FailureAnalysis performAnalysis(Class<?> configuration, String... environment) {
		BeanCreationException failure = createFailure(configuration, environment);
		assertThat(failure).isNotNull();
		return new BindFailureAnalyzer().analyze(failure);
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

	@EnableConfigurationProperties(BindValidationFailureAnalyzerTests.FieldValidationFailureProperties.class)
	static class FieldValidationFailureConfiguration {

	}

	@EnableConfigurationProperties(UnboundElementsFailureProperties.class)
	static class UnboundElementsFailureConfiguration {

	}

	@EnableConfigurationProperties(GenericFailureProperties.class)
	static class GenericFailureConfiguration {

	}

	@EnableConfigurationProperties(EnumFailureProperties.class)
	static class EnumFailureConfiguration {

	}

	@EnableConfigurationProperties(NestedFailureProperties.class)
	static class NestedFailureConfiguration {

	}

	@EnableConfigurationProperties(LoggingProperties.class)
	static class LoggingLevelFailureConfiguration {

	}

	@ConfigurationProperties("test.foo")
	@Validated
	static class FieldValidationFailureProperties {

		@Min(value = 5, message = "at least five")
		private int value;

		int getValue() {
			return this.value;
		}

		void setValue(int value) {
			this.value = value;
		}

	}

	@ConfigurationProperties("test.foo")
	static class UnboundElementsFailureProperties {

		private List<String> listValue;

		List<String> getListValue() {
			return this.listValue;
		}

		void setListValue(List<String> listValue) {
			this.listValue = listValue;
		}

	}

	@ConfigurationProperties("test.foo")
	static class GenericFailureProperties {

		private int value;

		private Class<?> type;

		int getValue() {
			return this.value;
		}

		void setValue(int value) {
			this.value = value;
		}

		Class<?> getType() {
			return this.type;
		}

		void setType(Class<?> type) {
			this.type = type;
		}

	}

	@ConfigurationProperties("test.foo")
	static class EnumFailureProperties {

		private Set<Fruit> fruit;

		Set<Fruit> getFruit() {
			return this.fruit;
		}

		void setFruit(Set<Fruit> fruit) {
			this.fruit = fruit;
		}

	}

	@ConfigurationProperties("test.foo")
	static class NestedFailureProperties {

		private String value;

		String getValue() {
			return this.value;
		}

		void setValue(String value) {
			throw new RuntimeException("This is a failure");
		}

	}

	@ConfigurationProperties("logging")
	static class LoggingProperties {

		private Map<String, LogLevel> level = new HashMap<>();

		Map<String, LogLevel> getLevel() {
			return this.level;
		}

		void setLevel(Map<String, LogLevel> level) {
			this.level = level;
		}

	}

	enum Fruit {

		APPLE, BANANA, ORANGE

	}

}
