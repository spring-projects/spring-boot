/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.StandardStackTracePrinter;
import org.springframework.boot.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import org.springframework.boot.util.Instantiator.AvailableParameters;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StructuredLogFormatterFactory}.
 *
 * @author Phillip Webb
 */
class StructuredLogFormatterFactoryTests {

	private final StructuredLogFormatterFactory<LogEvent> factory;

	private final MockEnvironment environment = new MockEnvironment();

	StructuredLogFormatterFactoryTests() {
		this.environment.setProperty("logging.structured.ecs.service.version", "1.2.3");
		this.factory = new StructuredLogFormatterFactory<>(LogEvent.class, this.environment,
				this::addAvailableParameters, this::addCommonFormatters);
	}

	private void addAvailableParameters(AvailableParameters availableParameters) {
		availableParameters.add(StringBuilder.class, new StringBuilder("Hello"));
	}

	private void addCommonFormatters(CommonFormatters<LogEvent> commonFormatters) {
		commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA, (instantiator) -> {
			Environment environment = instantiator.getArg(Environment.class);
			assertThat(environment).isNotNull();
			StackTracePrinter stackTracePrinter = instantiator.getArg(StackTracePrinter.class);
			StringBuilder stringBuilder = instantiator.getArg(StringBuilder.class);
			assertThat(stringBuilder).isNotNull();
			return new TestEcsFormatter(environment, stackTracePrinter, stringBuilder);
		});
	}

	@Test
	void getUsingCommonFormat() {
		assertThat(this.factory.get("ecs")).isInstanceOf(TestEcsFormatter.class);
	}

	@Test
	void getUsingClassName() {
		assertThat(this.factory.get(ExtendedTestEcsFormatter.class.getName()))
			.isInstanceOf(ExtendedTestEcsFormatter.class);
	}

	@Test
	void getUsingClassNameWhenNoSuchClass() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(this.factory.get("com.example.WeMadeItUp")).isNull())
			.withMessage("Unknown format 'com.example.WeMadeItUp'. "
					+ "Values can be a valid fully-qualified class name or one of the common formats: [ecs]");
	}

	@Test
	void getUsingClassNameWhenHasGenericMismatch() {
		assertThatIllegalStateException().isThrownBy(() -> this.factory.get(DifferentFormatter.class.getName()))
			.withMessage("Type argument of org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$DifferentFormatter "
					+ "must be org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$LogEvent "
					+ "but was org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$DifferentLogEvent");
	}

	@Test
	void getUsingClassNameInjectsEnvironment() {
		TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
		assertThat(formatter.getEnvironment()).isSameAs(this.environment);
	}

	@Test
	void getUsingClassNameInjectsStackTracePrinter() {
		this.environment.setProperty("logging.structured.json.stacktrace.printer", "standard");
		StructuredLogFormatterFactory<LogEvent> factory = new StructuredLogFormatterFactory<>(LogEvent.class,
				this.environment, this::addAvailableParameters, this::addCommonFormatters);
		TestEcsFormatter formatter = (TestEcsFormatter) factory.get(TestEcsFormatter.class.getName());
		assertThat(formatter.getStackTracePrinter()).isInstanceOf(StandardStackTracePrinter.class);
	}

	@Test
	void getUsingClassNameInjectsCustomParameter() {
		TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
		assertThat(formatter.getCustom()).hasToString("Hello");
	}

	@Test
	void getInjectStringMembersCustomizer() {
		this.environment.setProperty("logging.structured.json.rename.spring", "test");
		SpringFactoriesLoader factoriesLoader = mock(SpringFactoriesLoader.class);
		given(factoriesLoader.load(any(), any(ArgumentResolver.class)))
			.willReturn(List.of(new StringMembersStructuredLoggingJsonMembersCustomizer()));
		StructuredLogFormatterFactory<LogEvent> factory = new StructuredLogFormatterFactory<>(factoriesLoader,
				LogEvent.class, this.environment, this::addAvailableParameters, this::addCommonFormatters);
		CustomizedFormatter formatter = (CustomizedFormatter) factory.get(CustomizedFormatter.class.getName());
		assertThat(formatter.format(new LogEvent())).contains("\"test\":\"BOOT\"");
	}

	@Test
	void getInjectObjectMembersCustomizer() {
		this.environment.setProperty("logging.structured.json.rename.spring", "test");
		SpringFactoriesLoader factoriesLoader = mock(SpringFactoriesLoader.class);
		given(factoriesLoader.load(any(), any(ArgumentResolver.class)))
			.willReturn(List.of(new ObjectMembersStructuredLoggingJsonMembersCustomizer()));
		StructuredLogFormatterFactory<LogEvent> factory = new StructuredLogFormatterFactory<>(factoriesLoader,
				LogEvent.class, this.environment, this::addAvailableParameters, this::addCommonFormatters);
		CustomizedFormatter formatter = (CustomizedFormatter) factory.get(CustomizedFormatter.class.getName());
		assertThat(formatter.format(new LogEvent())).contains("\"test\":\"BOOT\"");
	}

	static class StringMembersStructuredLoggingJsonMembersCustomizer
			implements StructuredLoggingJsonMembersCustomizer<String> {

		@Override
		public void customize(Members<String> members) {
			UnaryOperator<@Nullable String> toUpperCase = (string) -> (string != null) ? string.toUpperCase(Locale.ROOT)
					: null;
			members.applyingValueProcessor(ValueProcessor.of(String.class, toUpperCase));
		}

	}

	static class ObjectMembersStructuredLoggingJsonMembersCustomizer
			implements StructuredLoggingJsonMembersCustomizer<Object> {

		@Override
		public void customize(Members<Object> members) {
			UnaryOperator<@Nullable String> toUpperCase = (string) -> (string != null) ? string.toUpperCase(Locale.ROOT)
					: null;
			members.applyingValueProcessor(ValueProcessor.of(String.class, toUpperCase));
		}

	}

	static class LogEvent {

	}

	static class DifferentLogEvent {

	}

	static class TestEcsFormatter implements StructuredLogFormatter<LogEvent> {

		private final Environment environment;

		private final @Nullable StackTracePrinter stackTracePrinter;

		private final StringBuilder custom;

		TestEcsFormatter(Environment environment, @Nullable StackTracePrinter stackTracePrinter, StringBuilder custom) {
			this.environment = environment;
			this.stackTracePrinter = stackTracePrinter;
			this.custom = custom;
		}

		@Override
		public String format(LogEvent event) {
			return "formatted " + this.environment.getProperty("logging.structured.ecs.service.version");
		}

		Environment getEnvironment() {
			return this.environment;
		}

		@Nullable StackTracePrinter getStackTracePrinter() {
			return this.stackTracePrinter;
		}

		StringBuilder getCustom() {
			return this.custom;
		}

	}

	static class ExtendedTestEcsFormatter extends TestEcsFormatter {

		ExtendedTestEcsFormatter(Environment environment, StackTracePrinter stackTracePrinter, StringBuilder custom) {
			super(environment, stackTracePrinter, custom);
		}

	}

	static class DifferentFormatter implements StructuredLogFormatter<DifferentLogEvent> {

		@Override
		public String format(DifferentLogEvent event) {
			return "";
		}

	}

	static class CustomizedFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

		CustomizedFormatter(StructuredLoggingJsonMembersCustomizer<?> customizer) {
			super((members) -> members.add("spring", "boot"), customizer);
		}

	}

}
