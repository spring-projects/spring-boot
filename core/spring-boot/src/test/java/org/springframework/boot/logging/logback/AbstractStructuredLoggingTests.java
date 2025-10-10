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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.slf4j.helpers.BasicMarkerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.boot.logging.structured.MockStructuredLoggingJsonMembersCustomizerBuilder;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for structured formatting tests.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractStructuredLoggingTests {

	static final Instant EVENT_TIME = Instant.ofEpochSecond(1719910193L);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ThrowableProxyConverter throwableProxyConverter;

	private BasicMarkerFactory markerFactory;

	@Mock
	@SuppressWarnings("NullAway.Init")
	StructuredLoggingJsonMembersCustomizer<?> customizer;

	MockStructuredLoggingJsonMembersCustomizerBuilder<?> customizerBuilder = new MockStructuredLoggingJsonMembersCustomizerBuilder<>(
			() -> this.customizer);

	@BeforeEach
	void setUp() {
		this.markerFactory = new BasicMarkerFactory();
		this.throwableProxyConverter = new ThrowableProxyConverter();
		this.throwableProxyConverter.start();
	}

	@AfterEach
	void tearDown() {
		this.throwableProxyConverter.stop();
	}

	protected Marker getMarker(String name) {
		return this.markerFactory.getDetachedMarker(name);
	}

	protected ThrowableProxyConverter getThrowableProxyConverter() {
		return this.throwableProxyConverter;
	}

	protected Map<String, Object> map(Object... values) {
		assertThat(values.length).isEven();
		Map<String, Object> result = new HashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			result.put(values[i].toString(), values[i + 1]);
		}
		return result;
	}

	protected List<KeyValuePair> keyValuePairs(Object... values) {
		assertThat(values.length).isEven();
		List<KeyValuePair> result = new ArrayList<>();
		for (int i = 0; i < values.length; i += 2) {
			result.add(new KeyValuePair(values[i].toString(), values[i + 1]));
		}
		return result;
	}

	protected static LoggingEvent createEvent() {
		return createEvent(null);
	}

	protected static LoggingEvent createEvent(@Nullable Throwable thrown) {
		LoggingEvent event = new LoggingEvent();
		event.setInstant(EVENT_TIME);
		event.setLevel(Level.INFO);
		event.setThreadName("main");
		event.setLoggerName("org.example.Test");
		event.setMessage("message");
		if (thrown != null) {
			event.setThrowableProxy(new ThrowableProxy(thrown));
		}
		return event;
	}

	protected Map<String, Object> deserialize(String json) {
		return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
		});
	}

}
