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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@BeforeEach
	void setUp() {
		this.environment = new MockEnvironment();
		this.environment.setProperty("logging.structured.ecs.service.name", "name");
		this.environment.setProperty("logging.structured.ecs.service.version", "1.0.0");
		this.environment.setProperty("logging.structured.ecs.service.environment", "test");
		this.environment.setProperty("logging.structured.ecs.service.node-name", "node-1");
		this.environment.setProperty("spring.application.pid", "1");
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, null,
				TestContextPairs.include(), this.customizer);
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
		Map<String, Object> expected = new HashMap<>();
		expected.put("@timestamp", "2024-07-02T08:49:53Z");
		expected.put("message", "message");
		expected.put("mdc-1", "mdc-v-1");
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
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		Map<String, Object> error = (Map<String, Object>) deserialized.get("error");
		Map<String, Object> expectedError = new HashMap<>();
		expectedError.put("type", "java.lang.RuntimeException");
		expectedError.put("message", "Boom");
		assertThat(error).containsAllEntriesOf(expectedError);
		String stackTrace = (String) error.get("stack_trace");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException""");
		assertThat(json).contains(
				"""
						java.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.log4j2.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException""");
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldFormatExceptionUsingStackTracePrinter() {
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, new SimpleStackTracePrinter(),
				TestContextPairs.include(), this.customizer);
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		Map<String, Object> deserialized = deserialize(this.formatter.format(event));
		Map<String, Object> error = (Map<String, Object>) deserialized.get("error");
		String stackTrace = (String) error.get("stack_trace");
		assertThat(stackTrace).isEqualTo("stacktrace:RuntimeException");
	}

	@Test
	void shouldFormatStructuredMessage() {
		MutableLogEvent event = createEvent();
		event.setMessage(new MapMessage<>().with("foo", true).with("bar", 1.0));
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		Map<String, Object> expectedMessage = Map.of("foo", true, "bar", 1.0);
		assertThat(deserialized.get("message")).isEqualTo(expectedMessage);
	}

	@Test
	void shouldFormatMarkersAsTags() {
		MutableLogEvent event = createEvent();
		Marker parent = MarkerManager.getMarker("parent");
		parent.addParents(MarkerManager.getMarker("grandparent"));
		Marker parent1 = MarkerManager.getMarker("parent1");
		parent1.addParents(MarkerManager.getMarker("grandparent1"));
		Marker grandchild = MarkerManager.getMarker("grandchild");
		grandchild.addParents(parent);
		grandchild.addParents(parent1);
		event.setMarker(grandchild);
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized.get("tags"))
			.isEqualTo(List.of("grandchild", "grandparent", "grandparent1", "parent", "parent1"));
	}

}
