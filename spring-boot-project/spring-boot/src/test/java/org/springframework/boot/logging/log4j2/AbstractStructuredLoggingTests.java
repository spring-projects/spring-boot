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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for structured formatting tests.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractStructuredLoggingTests {

	static final Instant EVENT_TIME = Instant.ofEpochMilli(1719910193000L);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Mock
	StructuredLoggingJsonMembersCustomizer<?> customizer;

	protected Map<String, Object> map(Object... values) {
		assertThat(values.length).isEven();
		Map<String, Object> result = new HashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			result.put(values[i].toString(), values[i + 1]);
		}
		return result;
	}

	protected static MutableLogEvent createEvent() {
		MutableLogEvent event = new MutableLogEvent();
		event.setTimeMillis(EVENT_TIME.toEpochMilli());
		event.setLevel(Level.INFO);
		event.setThreadName("main");
		event.setLoggerName("org.example.Test");
		event.setMessage(new SimpleMessage("message"));
		return event;
	}

	protected Map<String, Object> deserialize(String json) {
		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException ex) {
			Assertions.fail("Failed to deserialize JSON: " + json, ex);
			return null;
		}
	}

}
