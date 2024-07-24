/*
 * Copyright 2012-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import org.springframework.boot.util.Instantiator.AvailableParameters;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
		commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA,
				(instantiator) -> new TestEcsFormatter(instantiator.getArg(Environment.class),
						instantiator.getArg(StringBuilder.class)));
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
		assertThatIllegalArgumentException().isThrownBy(() -> this.factory.get(DifferentFormatter.class.getName()))
			.withMessage("Type argument of org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$DifferentFormatter "
					+ "must be org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$LogEvent "
					+ "but was org.springframework.boot.logging.structured."
					+ "StructuredLogFormatterFactoryTests$DifferentLogEvent");
	}

	@Test
	void getUsingClassNameInjectsApplicationMetadata() {
		TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
		assertThat(formatter.getEnvironment()).isSameAs(this.environment);
	}

	@Test
	void getUsingClassNameInjectsCustomParameter() {
		TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
		assertThat(formatter.getCustom()).hasToString("Hello");
	}

	static class LogEvent {

	}

	static class DifferentLogEvent {

	}

	static class TestEcsFormatter implements StructuredLogFormatter<LogEvent> {

		private Environment environment;

		private StringBuilder custom;

		TestEcsFormatter(Environment environment, StringBuilder custom) {
			this.environment = environment;
			this.custom = custom;
		}

		@Override
		public String format(LogEvent event) {
			return "formatted " + this.environment.getProperty("logging.structured.ecs.service.version");
		}

		Environment getEnvironment() {
			return this.environment;
		}

		StringBuilder getCustom() {
			return this.custom;
		}

	}

	static class ExtendedTestEcsFormatter extends TestEcsFormatter {

		ExtendedTestEcsFormatter(Environment environment, StringBuilder custom) {
			super(environment, custom);
		}

	}

	static class DifferentFormatter implements StructuredLogFormatter<DifferentLogEvent> {

		@Override
		public String format(DifferentLogEvent event) {
			return "";
		}

	}

}
