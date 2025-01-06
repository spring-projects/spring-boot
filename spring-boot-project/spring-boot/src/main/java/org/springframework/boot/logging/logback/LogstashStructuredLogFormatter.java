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

package org.springframework.boot.logging.logback;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Logback {@link StructuredLogFormatter} for {@link CommonStructuredLogFormat#LOGSTASH}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class LogstashStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
			(pair) -> pair.value);

	LogstashStructuredLogFormatter(ThrowableProxyConverter throwableProxyConverter,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(throwableProxyConverter, members), customizer);
	}

	private static void jsonMembers(ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("@timestamp", ILoggingEvent::getInstant).as(LogstashStructuredLogFormatter::asTimestamp);
		members.add("@version", "1");
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.add("logger_name", ILoggingEvent::getLoggerName);
		members.add("thread_name", ILoggingEvent::getThreadName);
		members.add("level", ILoggingEvent::getLevel);
		members.add("level_value", ILoggingEvent::getLevel).as(Level::toInt);
		members.addMapEntries(ILoggingEvent::getMDCPropertyMap);
		members.from(ILoggingEvent::getKeyValuePairs)
			.whenNotEmpty()
			.usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);
		members.add("tags", ILoggingEvent::getMarkerList)
			.whenNotNull()
			.as(LogstashStructuredLogFormatter::getMarkers)
			.whenNotEmpty();
		members.add("stack_trace", (event) -> event)
			.whenNotNull(ILoggingEvent::getThrowableProxy)
			.as(throwableProxyConverter::convert);
	}

	private static String asTimestamp(Instant instant) {
		OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
	}

	private static Set<String> getMarkers(List<Marker> markers) {
		Set<String> result = new LinkedHashSet<>();
		addMarkers(result, markers.iterator());
		return result;
	}

	private static void addMarkers(Set<String> result, Iterator<Marker> iterator) {
		while (iterator.hasNext()) {
			Marker marker = iterator.next();
			result.add(marker.getName());
			if (marker.hasReferences()) {
				addMarkers(result, marker.iterator());
			}
		}
	}

}
