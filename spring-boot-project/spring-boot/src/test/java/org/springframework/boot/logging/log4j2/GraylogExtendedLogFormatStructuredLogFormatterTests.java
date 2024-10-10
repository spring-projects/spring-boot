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

package org.springframework.boot.logging.log4j2;

import java.util.Map;

import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link GraylogExtendedLogFormatStructuredLogFormatter}.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class GraylogExtendedLogFormatStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

	private GraylogExtendedLogFormatStructuredLogFormatter formatter;

	@BeforeEach
	void setUp() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.gelf.host", "name");
		environment.setProperty("logging.structured.gelf.service.version", "1.0.0");
		environment.setProperty("spring.application.pid", "1");
		this.formatter = new GraylogExtendedLogFormatStructuredLogFormatter(environment, this.customizer);
	}

	@Test
	void callsCustomizer() {
		then(this.customizer).should().customize(any());
	}

	@Test
	void shouldFormat() {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("mdc-1", "mdc-v-1"), true));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
				map("version", "1.1", "host", "name", "timestamp", 1719910193.0, "level", 6, "_level_name", "INFO",
						"_process_pid", 1, "_process_thread_name", "main", "_service_version", "1.0.0", "_log_logger",
						"org.example.Test", "short_message", "message", "_mdc-1", "mdc-v-1"));
	}

	@Test
	void shouldFormatMillisecondsInTimestamp() {
		MutableLogEvent event = createEvent();
		event.setTimeMillis(1719910193123L);
		String json = this.formatter.format(event);
		assertThat(json).contains("\"timestamp\":1719910193.123");
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
				1719910193.123, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
				"_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
	}

	@Test
	void shouldNotAllowInvalidFieldNames(CapturedOutput output) {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("/", "value"), true));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
				1719910193.0, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
				"_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
		assertThat(output).contains("'/' is not a valid field name according to GELF standard");
	}

	@Test
	void shouldNotAllowIllegalFieldNames(CapturedOutput output) {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("id", "1"), true));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
				1719910193.0, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
				"_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
		assertThat(output).contains("'id' is an illegal field name according to GELF standard");
	}

	@Test
	void shouldNotAddDoubleUnderscoreToCustomFields() {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("_custom", "value"), true));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
				map("version", "1.1", "host", "name", "timestamp", 1719910193.0, "level", 6, "_level_name", "INFO",
						"_process_pid", 1, "_process_thread_name", "main", "_service_version", "1.0.0", "_log_logger",
						"org.example.Test", "short_message", "message", "_custom", "value"));
	}

	@Test
	void shouldFormatException() {
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String fullMessage = (String) deserialized.get("full_message");
		String stackTrace = (String) deserialized.get("_error_stack_trace");
		assertThat(fullMessage).startsWith(
				"""
						message

						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException""");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException""");

		assertThat(deserialized)
			.containsAllEntriesOf(map("_error_type", "java.lang.RuntimeException", "_error_message", "Boom"));
		assertThat(json).contains(
				"""
						message\\n\\njava.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.log4j2.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException""");
	}

}
