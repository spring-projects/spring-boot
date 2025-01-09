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

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ElasticCommonSchemaProperties;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * Log4j2 {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#ELASTIC_COMMON_SCHEMA}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticCommonSchemaStructuredLogFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

	ElasticCommonSchemaStructuredLogFormatter(Environment environment,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(environment, members), customizer);
	}

	private static void jsonMembers(Environment environment, JsonWriter.Members<LogEvent> members) {
		members.add("@timestamp", LogEvent::getInstant).as(ElasticCommonSchemaStructuredLogFormatter::asTimestamp);
		members.add("log.level", LogEvent::getLevel).as(Level::name);
		members.add("process.pid", environment.getProperty("spring.application.pid", Long.class))
			.when(Objects::nonNull);
		members.add("process.thread.name", LogEvent::getThreadName);
		ElasticCommonSchemaProperties.get(environment).jsonMembers(members);
		members.add("log.logger", LogEvent::getLoggerName);
		members.add("message", LogEvent::getMessage).as(StructuredMessage::get);
		members.from(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.usingPairs((contextData, pairs) -> contextData.forEach(pairs::accept));
		members.from(LogEvent::getThrownProxy).whenNotNull().usingMembers((thrownProxyMembers) -> {
			thrownProxyMembers.add("error.type", ThrowableProxy::getThrowable)
				.whenNotNull()
				.as(ObjectUtils::nullSafeClassName);
			thrownProxyMembers.add("error.message", ThrowableProxy::getMessage);
			thrownProxyMembers.add("error.stack_trace", ThrowableProxy::getExtendedStackTraceAsString);
		});
		members.add("tags", LogEvent::getMarker)
			.whenNotNull()
			.as(ElasticCommonSchemaStructuredLogFormatter::getMarkers)
			.whenNotEmpty();
		members.add("ecs.version", "8.11");
	}

	private static java.time.Instant asTimestamp(Instant instant) {
		return java.time.Instant.ofEpochMilli(instant.getEpochMillisecond()).plusNanos(instant.getNanoOfMillisecond());
	}

	private static Set<String> getMarkers(Marker marker) {
		Set<String> result = new TreeSet<>();
		addMarkers(result, marker);
		return result;
	}

	private static void addMarkers(Set<String> result, Marker marker) {
		result.add(marker.getName());
		if (marker.hasParents()) {
			for (Marker parent : marker.getParents()) {
				addMarkers(result, parent);
			}
		}
	}

}
