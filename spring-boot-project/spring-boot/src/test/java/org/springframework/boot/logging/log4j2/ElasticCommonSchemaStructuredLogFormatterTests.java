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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, null, this.customizer);
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
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
				"log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
				"service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
				"org.example.Test", "message", "message", "mdc-1", "mdc-v-1", "ecs.version", "8.11"));
	}

	@Test
	void shouldFormatException() {
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized)
			.containsAllEntriesOf(map("error.type", "java.lang.RuntimeException", "error.message", "Boom"));
		String stackTrace = (String) deserialized.get("error.stack_trace");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException""");
		assertThat(json).contains(
				"""
						java.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.log4j2.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException""");
	}

	@Test
	void shouldFormatExceptionUsingStackTracePrinter() {
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(this.environment, new SimpleStackTracePrinter(),
				this.customizer);
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		Map<String, Object> deserialized = deserialize(this.formatter.format(event));
		String stackTrace = (String) deserialized.get("error.stack_trace");
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
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
				"log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
				"service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
				"org.example.Test", "message", expectedMessage, "ecs.version", "8.11"));
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
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
				"log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
				"service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
				"org.example.Test", "message", "message", "ecs.version", "8.11", "tags",
				List.of("grandchild", "grandparent", "grandparent1", "parent", "parent1")));
	}

}
