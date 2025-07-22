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

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import org.springframework.boot.logging.structured.TestContextPairs;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link ElasticCommonSchemaStructuredLogFormatter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticCommonSchemaStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

	private MockEnvironment environment;

	private ElasticCommonSchemaStructuredLogFormatter formatter;

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		this.environment = new MockEnvironment();
		this.environment.setProperty("logging.structured.ecs.service.name", "name");
		this.environment.setProperty("logging.structured.ecs.service.version", "1.0.0");
		this.environment.setProperty("logging.structured.ecs.service.environment", "test");
		this.environment.setProperty("logging.structured.ecs.service.node-name", "node-1");
		this.environment.setProperty("spring.application.pid", "1");
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, null,
				TestContextPairs.include(), getThrowableProxyConverter(), this.customizerBuilder);
	}

	@Test
	void callsNestedOnCustomizerBuilder() {
		assertThat(this.customizerBuilder.isNested()).isTrue();
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
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		Map<String, Object> expected = new HashMap<>();
		expected.put("@timestamp", "2024-07-02T08:49:53Z");
		expected.put("message", "message");
		expected.put("mdc-1", "mdc-v-1");
		expected.put("kv-1", "kv-v-1");
		expected.put("ecs", Map.of("version", "8.11"));
		expected.put("process", map("pid", 1, "thread", map("name", "main")));
		expected.put("log", map("level", "INFO", "logger", "org.example.Test"));
		expected.put("service",
				map("name", "name", "version", "1.0.0", "environment", "test", "node", map("name", "node-1")));
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(expected);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldFormatException() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		Map<String, Object> error = (Map<String, Object>) deserialized.get("error");
		Map<String, Object> expectedError = new HashMap<>();
		expectedError.put("type", "java.lang.RuntimeException");
		expectedError.put("message", "Boom");
		assertThat(error).containsAllEntriesOf(expectedError);
		String stackTrace = (String) error.get("stack_trace");
		assertThat(stackTrace)
			.startsWith(String.format("java.lang.RuntimeException: Boom%n\tat org.springframework.boot.logging.logback."
					+ "ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException"));
		assertThat(json).contains(String
			.format("java.lang.RuntimeException: Boom%n\\tat org.springframework.boot.logging.logback."
					+ "ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException")
			.replace("\n", "\\n")
			.replace("\r", "\\r"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldFormatExceptionUsingStackTracePrinter() {
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, new SimpleStackTracePrinter(),
				TestContextPairs.include(), getThrowableProxyConverter(), this.customizerBuilder);
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		Map<String, Object> deserialized = deserialize(this.formatter.format(event));
		Map<String, Object> error = (Map<String, Object>) deserialized.get("error");
		String stackTrace = (String) error.get("stack_trace");
		assertThat(stackTrace).isEqualTo("stacktrace:RuntimeException");
	}

	@Test
	void shouldFormatMarkersAsTags() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		Marker parent = MarkerFactory.getDetachedMarker("parent");
		parent.add(MarkerFactory.getDetachedMarker("child"));
		Marker parent1 = MarkerFactory.getDetachedMarker("parent1");
		parent1.add(MarkerFactory.getDetachedMarker("child1"));
		Marker grandparent = MarkerFactory.getMarker("grandparent");
		grandparent.add(parent);
		grandparent.add(parent1);
		event.addMarker(grandparent);
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized.get("tags")).isEqualTo(List.of("child", "child1", "grandparent", "parent", "parent1"));
	}

	@Test
	void shouldNestMdcAndKeyValuePairs() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Map.of("a1.b1.c1", "A1B1C1", "a1.b1.c2", "A1B1C2"));
		event.setKeyValuePairs(keyValuePairs("a2.b1.c1", "A2B1C1", "a2.b1.c2", "A2B1C2"));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized.get("a1")).isEqualTo(Map.of("b1", Map.of("c1", "A1B1C1", "c2", "A1B1C2")));
		assertThat(deserialized.get("a2")).isEqualTo(Map.of("b1", Map.of("c1", "A2B1C1", "c2", "A2B1C2")));
	}

}
