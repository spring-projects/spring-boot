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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.MarkerManager.Log4jMarker;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link LogstashStructuredLogFormatter}.
 *
 * @author Moritz Halbritter
 */
class LogstashStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

	private LogstashStructuredLogFormatter formatter;

	@BeforeEach
	void setUp() {
		this.formatter = new LogstashStructuredLogFormatter(this.customizer);
	}

	@Test
	void callsCustomizer() {
		then(this.customizer).should().customize(any());
	}

	@Test
	void shouldFormat() {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("mdc-1", "mdc-v-1"), true));
		Log4jMarker marker1 = new Log4jMarker("marker-1");
		marker1.addParents(new Log4jMarker("marker-2"));
		event.setMarker(marker1);
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
			.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", timestamp, "@version", "1",
				"message", "message", "logger_name", "org.example.Test", "thread_name", "main", "level", "INFO",
				"level_value", 400, "mdc-1", "mdc-v-1", "tags", List.of("marker-1", "marker-2")));
	}

	@Test
	void shouldFormatException() {
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String stackTrace = (String) deserialized.get("stack_trace");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.LogstashStructuredLogFormatterTests.shouldFormatException""");
		assertThat(json).contains(
				"""
						java.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.log4j2.LogstashStructuredLogFormatterTests.shouldFormatException""");
	}

	@Test
	void shouldFormatStructuredMessage() {
		MutableLogEvent event = createEvent();
		event.setMessage(new MapMessage<>().with("foo", true).with("bar", 1.0));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		Map<String, Object> expectedMessage = Map.of("foo", true, "bar", 1.0);
		String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
			.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
				map("@timestamp", timestamp, "@version", "1", "message", expectedMessage, "logger_name",
						"org.example.Test", "thread_name", "main", "level", "INFO", "level_value", 400));
	}

}
