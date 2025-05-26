/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.StandardStackTracePrinter;
import org.springframework.boot.logging.TestException;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.Context;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StackTrace;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StackTrace.Root;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StructuredLoggingJsonPropertiesRuntimeHints;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StructuredLoggingJsonProperties}.
 *
 * @author Phillip Webb
 */
class StructuredLoggingJsonPropertiesTests {

	@Test
	void getWhenHasNoStackTracePropertiesBindsFromEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		setupJsonProperties(environment);
		StructuredLoggingJsonProperties properties = StructuredLoggingJsonProperties.get(environment);
		assertThat(properties).isEqualTo(new StructuredLoggingJsonProperties(Set.of("a", "b"), Set.of("c", "d"),
				Map.of("e", "f"), Map.of("g", "h"), null, null, Set.of(TestCustomizer.class)));
	}

	@Test
	void getWhenHasStackTracePropertiesBindsFromEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		setupJsonProperties(environment);
		environment.setProperty("logging.structured.json.stacktrace.printer", "standard");
		environment.setProperty("logging.structured.json.stacktrace.root", "first");
		environment.setProperty("logging.structured.json.stacktrace.max-length", "1024");
		environment.setProperty("logging.structured.json.stacktrace.max-throwable-depth", "5");
		environment.setProperty("logging.structured.json.stacktrace.include-common-frames", "true");
		environment.setProperty("logging.structured.json.stacktrace.include-hashes", "true");
		StructuredLoggingJsonProperties properties = StructuredLoggingJsonProperties.get(environment);
		assertThat(properties.stackTrace())
			.isEqualTo(new StructuredLoggingJsonProperties.StackTrace("standard", Root.FIRST, 1024, 5, true, true));
	}

	private void setupJsonProperties(MockEnvironment environment) {
		environment.setProperty("logging.structured.json.include", "a,b");
		environment.setProperty("logging.structured.json.exclude", "c,d");
		environment.setProperty("logging.structured.json.rename.e", "f");
		environment.setProperty("logging.structured.json.add.g", "h");
		environment.setProperty("logging.structured.json.customizer", TestCustomizer.class.getName());
	}

	@Test
	void getWhenNoBoundPropertiesReturnsNull() {
		MockEnvironment environment = new MockEnvironment();
		StructuredLoggingJsonProperties.get(environment);
	}

	@Test
	void shouldRegisterRuntimeHints() throws Exception {
		RuntimeHints hints = new RuntimeHints();
		new StructuredLoggingJsonPropertiesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(StructuredLoggingJsonProperties.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructor(StructuredLoggingJsonProperties.class.getDeclaredConstructor(Set.class, Set.class, Map.class,
					Map.class, StackTrace.class, Context.class, Set.class))
			.invoke()).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructor(StackTrace.class.getDeclaredConstructor(String.class, Root.class, Integer.class,
					Integer.class, Boolean.class, Boolean.class))
			.invoke()).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructor(Context.class.getDeclaredConstructor(boolean.class, String.class))
			.invoke()).accepts(hints);
	}

	@Test
	void structuredLoggingJsonPropertiesRuntimeHintsIsRegistered() {
		assertThat(AotServices.factories().load(RuntimeHintsRegistrar.class))
			.anyMatch(StructuredLoggingJsonPropertiesRuntimeHints.class::isInstance);
	}

	@Nested
	class StackTraceTests {

		@Test
		void createPrinterWhenEmptyReturnsNull() {
			StackTrace properties = new StackTrace(null, null, null, null, null, null);
			assertThat(properties.createPrinter()).isNull();
		}

		@Test
		void createPrinterWhenNoPrinterAndNotEmptyReturnsStandard() {
			StackTrace properties = new StackTrace(null, Root.LAST, null, null, null, null);
			assertThat(properties.createPrinter()).isInstanceOf(StandardStackTracePrinter.class);
		}

		@Test
		void createPrinterWhenLoggingSystemReturnsNull() {
			StackTrace properties = new StackTrace("logging-system", null, null, null, null, null);
			assertThat(properties.createPrinter()).isNull();
		}

		@Test
		void createPrinterWhenLoggingSystemRelaxedReturnsNull() {
			StackTrace properties = new StackTrace("LoggingSystem", null, null, null, null, null);
			assertThat(properties.createPrinter()).isNull();
		}

		@Test
		void createPrinterWhenStandardReturnsStandardPrinter() {
			StackTrace properties = new StackTrace("standard", null, null, null, null, null);
			assertThat(properties.createPrinter()).isInstanceOf(StandardStackTracePrinter.class);
		}

		@Test
		void createPrinterWhenStandardRelaxedReturnsStandardPrinter() {
			StackTrace properties = new StackTrace("STANDARD", null, null, null, null, null);
			assertThat(properties.createPrinter()).isInstanceOf(StandardStackTracePrinter.class);
		}

		@Test
		void createPrinterWhenStandardAppliesCustomizations() {
			Exception exception = TestException.create();
			StackTrace properties = new StackTrace(null, Root.FIRST, 300, 2, true, false);
			StackTracePrinter printer = ((StandardStackTracePrinter) properties.createPrinter())
				.withLineSeparator("\n");
			String actual = TestException.withoutLineNumbers(printer.printStackTraceToString(exception));
			assertThat(actual).isEqualToNormalizingNewlines("""
					java.lang.RuntimeException: root
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:NN)
						at org.springframework.boot.logging.TestException$CreatorThread.run(TestException.java:NN)
					Wrapped by: java.lang.RuntimeException: cause
						at org.springframework.boot.log...""");
		}

		@Test
		void createPrinterWhenStandardWithHashesPrintsHash() {
			Exception exception = TestException.create();
			StackTrace properties = new StackTrace(null, null, null, null, null, true);
			StackTracePrinter printer = properties.createPrinter();
			String actual = printer.printStackTraceToString(exception);
			assertThat(actual).containsPattern("<#[0-9a-z]{8}>");
		}

		@Test
		void createPrinterWhenClassNameCreatesPrinter() {
			Exception exception = TestException.create();
			StackTrace properties = new StackTrace(TestStackTracePrinter.class.getName(), null, null, null, true, null);
			StackTracePrinter printer = properties.createPrinter();
			assertThat(printer.printStackTraceToString(exception)).isEqualTo("java.lang.RuntimeException: exception");
		}

		@Test
		void createPrinterWhenClassNameInjectsConfiguredPrinter() {
			Exception exception = TestException.create();
			StackTrace properties = new StackTrace(TestStackTracePrinterCustomized.class.getName(), Root.FIRST, 300, 2,
					true, null);
			StackTracePrinter printer = properties.createPrinter();
			String actual = TestException.withoutLineNumbers(printer.printStackTraceToString(exception));
			assertThat(actual).isEqualTo("RuntimeExceptionroot!	at org.springfr...");
		}

		@Test
		void hasCustomPrinterShouldReturnFalseWhenPrinterIsEmpty() {
			StackTrace stackTrace = new StackTrace("", null, null, null, null, null);
			assertThat(stackTrace.hasCustomPrinter()).isFalse();
		}

		@Test
		void hasCustomPrinterShouldReturnFalseWhenPrinterHasLoggingSystem() {
			StackTrace stackTrace = new StackTrace("loggingsystem", null, null, null, null, null);
			assertThat(stackTrace.hasCustomPrinter()).isFalse();
		}

		@Test
		void hasCustomPrinterShouldReturnFalseWhenPrinterHasStandard() {
			StackTrace stackTrace = new StackTrace("standard", null, null, null, null, null);
			assertThat(stackTrace.hasCustomPrinter()).isFalse();
		}

		@Test
		void hasCustomPrinterShouldReturnTrueWhenPrinterHasCustom() {
			StackTrace stackTrace = new StackTrace("custom-printer", null, null, null, null, null);
			assertThat(stackTrace.hasCustomPrinter()).isTrue();
		}

	}

	static class TestCustomizer implements StructuredLoggingJsonMembersCustomizer<String> {

		@Override
		public void customize(Members<String> members) {
		}

	}

	static class TestStackTracePrinter implements StackTracePrinter {

		@Override
		public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
			out.append(throwable.toString());
		}

	}

	static class TestStackTracePrinterCustomized implements StackTracePrinter {

		private final StandardStackTracePrinter printer;

		TestStackTracePrinterCustomized(StandardStackTracePrinter printer) {
			this.printer = printer.withMaximumLength(40)
				.withLineSeparator("!")
				.withFormatter((throwable) -> ClassUtils.getShortName(throwable.getClass()) + throwable.getMessage());
		}

		@Override
		public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
			this.printer.printStackTrace(throwable, out);
		}

	}

}
