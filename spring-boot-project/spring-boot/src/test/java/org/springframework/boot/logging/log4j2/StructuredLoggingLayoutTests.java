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

package org.springframework.boot.logging.log4j2;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.log4j2.StructuredLogLayout.Builder;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link StructuredLogLayout}.
 *
 * @author Moritz Halbritter
 */
class StructuredLoggingLayoutTests extends AbstractStructuredLoggingTests {

	private MockEnvironment environment;

	private LoggerContext loggerContext;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.loggerContext = (LoggerContext) LogManager.getContext(false);
		this.loggerContext.putObject(Log4J2LoggingSystem.ENVIRONMENT_KEY, this.environment);
	}

	@AfterEach
	void cleanup() {
		this.loggerContext.removeObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
	}

	@Test
	void shouldSupportEcsCommonFormat() {
		StructuredLogLayout layout = newBuilder().setFormat("ecs").build();
		String json = layout.toSerializable(createEvent());
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("ecs.version");
	}

	@Test
	void shouldSupportLogstashCommonFormat() {
		StructuredLogLayout layout = newBuilder().setFormat("logstash").build();
		String json = layout.toSerializable(createEvent());
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("@version");
	}

	@Test
	void shouldSupportCustomFormat() {
		StructuredLogLayout layout = newBuilder().setFormat(CustomLog4j2StructuredLoggingFormatter.class.getName())
			.build();
		String format = layout.toSerializable(createEvent());
		assertThat(format).isEqualTo("custom-format");
	}

	@Test
	void shouldInjectCustomFormatConstructorParameters() {
		this.environment.setProperty("spring.application.pid", "42");
		StructuredLogLayout layout = newBuilder()
			.setFormat(CustomLog4j2StructuredLoggingFormatterWithInjection.class.getName())
			.build();
		String format = layout.toSerializable(createEvent());
		assertThat(format).isEqualTo("custom-format-with-injection pid=42");
	}

	@Test
	void shouldCheckTypeArgument() {
		assertThatIllegalStateException().isThrownBy(
				() -> newBuilder().setFormat(CustomLog4j2StructuredLoggingFormatterWrongType.class.getName()).build())
			.withMessageContaining("must be org.apache.logging.log4j.core.LogEvent but was java.lang.String");
	}

	@Test
	void shouldCheckTypeArgumentWithRawType() {
		assertThatIllegalStateException()
			.isThrownBy(
					() -> newBuilder().setFormat(CustomLog4j2StructuredLoggingFormatterRawType.class.getName()).build())
			.withMessageContaining("must be org.apache.logging.log4j.core.LogEvent but was null");
	}

	@Test
	void shouldFailIfNoCommonOrCustomFormatIsSet() {
		assertThatIllegalArgumentException().isThrownBy(() -> newBuilder().setFormat("does-not-exist").build())
			.withMessageContaining("Unknown format 'does-not-exist'. "
					+ "Values can be a valid fully-qualified class name or one of the common formats: [ecs, gelf, logstash]");
	}

	private Builder newBuilder() {
		Builder builder = StructuredLogLayout.newBuilder();
		ReflectionTestUtils.setField(builder, "loggerContext", this.loggerContext);
		return builder;
	}

	static final class CustomLog4j2StructuredLoggingFormatter implements StructuredLogFormatter<LogEvent> {

		@Override
		public String format(LogEvent event) {
			return "custom-format";
		}

	}

	static final class CustomLog4j2StructuredLoggingFormatterWithInjection implements StructuredLogFormatter<LogEvent> {

		private final Environment environment;

		CustomLog4j2StructuredLoggingFormatterWithInjection(Environment environment) {
			this.environment = environment;
		}

		@Override
		public String format(LogEvent event) {
			return "custom-format-with-injection pid=" + this.environment.getProperty("spring.application.pid");
		}

	}

	static final class CustomLog4j2StructuredLoggingFormatterWrongType implements StructuredLogFormatter<String> {

		@Override
		public String format(String event) {
			return event;
		}

	}

	@SuppressWarnings("rawtypes")
	static final class CustomLog4j2StructuredLoggingFormatterRawType implements StructuredLogFormatter {

		@Override
		public String format(Object event) {
			return "";
		}

	}

}
