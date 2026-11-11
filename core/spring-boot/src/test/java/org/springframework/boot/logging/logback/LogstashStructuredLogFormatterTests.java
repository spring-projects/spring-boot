/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import org.springframework.boot.logging.structured.TestContextPairs;

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

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		this.formatter = new LogstashStructuredLogFormatter(null, TestContextPairs.include(),
				getThrowableProxyConverter(), this.customizer);
	}

	@Test
	void callsCustomizer() {
		then(this.customizer).should().customize(any());
	}

	@Test
	void shouldFormat() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
		event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
		event.addMarker(getMarker("marker-1"));
		event.addMarker(getMarker("marker-2"));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
			.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", timestamp, "@version", "1",
				"message", "message", "logger_name", "org.example.Test", "thread_name", "main", "level", "INFO",
				"level_value", 20000, "mdc-1", "mdc-v-1", "kv-1", "kv-v-1", "tags", List.of("marker-1", "marker-2")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void shouldFormatWithNestedMarker() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
		Marker marker1 = getMarker("marker-1");
		marker1.add(getMarker("marker-2"));
		event.addMarker(marker1);
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
			.format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", timestamp, "@version", "1",
				"message", "message", "logger_name", "org.example.Test", "thread_name", "main", "level", "INFO",
				"level_value", 20000, "mdc-1", "mdc-v-1", "tags", List.of("marker-1", "marker-2")));
	}

	@Test
	void shouldFormatException() {
		LoggingEvent event = createEvent();
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String stackTrace = (String) deserialized.get("stack_trace");
		assertThat(stackTrace).startsWith(
				"java.lang.RuntimeException: Boom%n\tat org.springframework.boot.logging.logback.LogstashStructuredLogFormatterTests.shouldFormatException"
					.formatted());
		assertThat(json).contains(
				"java.lang.RuntimeException: Boom%n\\tat org.springframework.boot.logging.logback.LogstashStructuredLogFormatterTests.shouldFormatException"
					.formatted()
					.replace("\n", "\\n")
					.replace("\r", "\\r"));
	}

	@Test
	void shouldFormatExceptionWithStackTracePrinter() {
		this.formatter = new LogstashStructuredLogFormatter(new SimpleStackTracePrinter(), TestContextPairs.include(),
				getThrowableProxyConverter(), this.customizer);
		LoggingEvent event = createEvent();
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String stackTrace = (String) deserialized.get("stack_trace");
		assertThat(stackTrace).isEqualTo("stacktrace:RuntimeException");
	}

}
