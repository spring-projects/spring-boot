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

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link ElasticCommonSchemaStructuredLogFormatter}.
 *
 * @author Moritz Halbritter
 */
class ElasticCommonSchemaStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

	private ElasticCommonSchemaStructuredLogFormatter formatter;

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.ecs.service.name", "name");
		environment.setProperty("logging.structured.ecs.service.version", "1.0.0");
		environment.setProperty("logging.structured.ecs.service.environment", "test");
		environment.setProperty("logging.structured.ecs.service.node-name", "node-1");
		environment.setProperty("spring.application.pid", "1");
		this.formatter = new ElasticCommonSchemaStructuredLogFormatter(environment, getThrowableProxyConverter(),
				this.customizer);
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
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
				"log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
				"service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
				"org.example.Test", "message", "message", "mdc-1", "mdc-v-1", "kv-1", "kv-v-1", "ecs.version", "8.11"));
	}

	@Test
	void shouldFormatException() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Collections.emptyMap());
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized)
			.containsAllEntriesOf(map("error.type", "java.lang.RuntimeException", "error.message", "Boom"));
		String stackTrace = (String) deserialized.get("error.stack_trace");
		assertThat(stackTrace).startsWith(
				"java.lang.RuntimeException: Boom%n\tat org.springframework.boot.logging.logback.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException"
					.formatted());
		assertThat(json).contains(
				"java.lang.RuntimeException: Boom%n\\tat org.springframework.boot.logging.logback.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException"
					.formatted()
					.replace("\n", "\\n")
					.replace("\r", "\\r"));
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
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
				map("@timestamp", "2024-07-02T08:49:53Z", "log.level", "INFO", "process.pid", 1, "process.thread.name",
						"main", "service.name", "name", "service.version", "1.0.0", "service.environment", "test",
						"service.node.name", "node-1", "log.logger", "org.example.Test", "message", "message",
						"ecs.version", "8.11", "tags", List.of("child", "child1", "grandparent", "parent", "parent1")));
	}

}
