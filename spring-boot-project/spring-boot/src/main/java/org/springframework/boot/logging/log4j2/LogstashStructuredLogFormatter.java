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
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.util.CollectionUtils;

/**
 * Log4j2 {@link StructuredLogFormatter} for {@link CommonStructuredLogFormat#LOGSTASH}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class LogstashStructuredLogFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

	LogstashStructuredLogFormatter(StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super(LogstashStructuredLogFormatter::jsonMembers, customizer);
	}

	private static void jsonMembers(JsonWriter.Members<LogEvent> members) {
		members.add("@timestamp", LogEvent::getInstant).as(LogstashStructuredLogFormatter::asTimestamp);
		members.add("@version", "1");
		members.add("message", LogEvent::getMessage).as(StructuredMessage::get);
		members.add("logger_name", LogEvent::getLoggerName);
		members.add("thread_name", LogEvent::getThreadName);
		members.add("level", LogEvent::getLevel).as(Level::name);
		members.add("level_value", LogEvent::getLevel).as(Level::intLevel);
		members.from(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.usingPairs((contextData, pairs) -> contextData.forEach(pairs::accept));
		members.add("tags", LogEvent::getMarker)
			.whenNotNull()
			.as(LogstashStructuredLogFormatter::getMarkers)
			.whenNot(CollectionUtils::isEmpty);
		members.add("stack_trace", LogEvent::getThrownProxy)
			.whenNotNull()
			.as(ThrowableProxy::getExtendedStackTraceAsString);
	}

	private static String asTimestamp(Instant instant) {
		java.time.Instant javaInstant = java.time.Instant.ofEpochMilli(instant.getEpochMillisecond())
			.plusNanos(instant.getNanoOfMillisecond());
		OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(javaInstant, ZoneId.systemDefault());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
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
