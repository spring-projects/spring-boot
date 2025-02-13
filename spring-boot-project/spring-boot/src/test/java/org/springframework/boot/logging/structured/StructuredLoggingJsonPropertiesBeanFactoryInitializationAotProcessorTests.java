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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Dmytro Nosan
 */
class StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessorTests {

	@Test
	void structuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
			.anyMatch(StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void shouldRegisterRuntimeHintsWhenCustomizerIsPresent() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.json.customizer", TestCustomizer.class.getName());

		BeanFactoryInitializationAotContribution contribution = getContribution(environment);
		assertThat(contribution).isNotNull();

		RuntimeHints hints = getRuntimeHints(contribution);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TestCustomizer.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
					MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
			.accepts(hints);
	}

	@Test
	void shouldRegisterRuntimeHintsWhenCustomStackTracePrinterIsPresent() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.json.stacktrace.printer", TestStackTracePrinter.class.getName());

		BeanFactoryInitializationAotContribution contribution = getContribution(environment);
		assertThat(contribution).isNotNull();

		RuntimeHints hints = getRuntimeHints(contribution);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TestStackTracePrinter.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
					MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
			.accepts(hints);
	}

	@ParameterizedTest
	@ValueSource(strings = { "logging-system", "standard" })
	void shouldNotRegisterRuntimeHintsWhenStackTracePrinterIsNotCustomImplementation(String printer) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.json.stacktrace.printer", printer);

		BeanFactoryInitializationAotContribution contribution = getContribution(environment);
		assertThat(contribution).isNull();
	}

	@Test
	void shouldNotRegisterRuntimeHintsWhenPropertiesAreNotSet() {
		MockEnvironment environment = new MockEnvironment();
		BeanFactoryInitializationAotContribution contribution = getContribution(environment);
		assertThat(contribution).isNull();
	}

	@Test
	void shouldNotRegisterRuntimeHintsWhenCustomizerAndPrinterAreNotSet() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.json.exclude", "something");
		BeanFactoryInitializationAotContribution contribution = getContribution(environment);
		assertThat(contribution).isNull();
	}

	private BeanFactoryInitializationAotContribution getContribution(ConfigurableEnvironment environment) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.setEnvironment(environment);
			context.refresh();
			return new StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessor()
				.processAheadOfTime(context.getBeanFactory());
		}
	}

	private RuntimeHints getRuntimeHints(BeanFactoryInitializationAotContribution contribution) {
		TestGenerationContext generationContext = new TestGenerationContext();
		contribution.applyTo(generationContext, null);
		return generationContext.getRuntimeHints();
	}

	static class TestCustomizer implements StructuredLoggingJsonMembersCustomizer<String> {

		@Override
		public void customize(Members<String> members) {
		}

	}

	static class TestStackTracePrinter implements StackTracePrinter {

		@Override
		public void printStackTrace(Throwable throwable, Appendable out) throws IOException {

		}

	}

}
