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

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraylogExtendedLogFormatStructuredLogFormatter}.
 *
 * @author Samuel Lissner
 */
class GraylogExtendedLogFormatStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

	private GraylogExtendedLogFormatStructuredLogFormatter formatter;

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.gelf.service.name", "name");
		environment.setProperty("logging.structured.gelf.service.version", "1.0.0");
		environment.setProperty("logging.structured.gelf.service.environment", "test");
		environment.setProperty("logging.structured.gelf.service.node-name", "node-1");
		environment.setProperty("spring.application.pid", "1");
		this.formatter = new GraylogExtendedLogFormatStructuredLogFormatter(environment, getThrowableProxyConverter());
	}

	@Test
	void shouldFormat() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
		event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
				1719910193.000D, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
				"_service_version", "1.0.0", "_service_environment", "test", "_service_node_name", "node-1",
				"_log_logger", "org.example.Test", "short_message", "message", "_mdc-1", "mdc-v-1", "_kv-1", "kv-v-1"));
	}

	@Test
	void shouldFormatException() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));

		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String fullMessage = (String) deserialized.get("full_message");
		String stackTrace = (String) deserialized.get("_error_stack_trace");

		assertThat(fullMessage).startsWith(
				"message\n\njava.lang.RuntimeException: Boom%n\tat org.springframework.boot.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
					.formatted());

		assertThat(deserialized)
			.containsAllEntriesOf(map("_error_type", "java.lang.RuntimeException", "_error_message", "Boom"));

		assertThat(stackTrace).startsWith(
				"java.lang.RuntimeException: Boom%n\tat org.springframework.boot.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
					.formatted());
		assertThat(json).contains(
				"java.lang.RuntimeException: Boom%n\\tat org.springframework.boot.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
					.formatted()
					.replace("\n", "\\n")
					.replace("\r", "\\r"));
	}

}
