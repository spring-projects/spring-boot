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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.ElasticCommonSchemaProperties;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#ELASTIC_COMMON_SCHEMA}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticCommonSchemaStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
			(pair) -> pair.value);

	ElasticCommonSchemaStructuredLogFormatter(Environment environment, StackTracePrinter stackTracePrinter,
			ContextPairs contextPairs, ThrowableProxyConverter throwableProxyConverter,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(environment, stackTracePrinter, contextPairs, throwableProxyConverter, members),
				customizer);
	}

	private static void jsonMembers(Environment environment, StackTracePrinter stackTracePrinter,
			ContextPairs contextPairs, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		Extractor extractor = new Extractor(stackTracePrinter, throwableProxyConverter);
		members.add("@timestamp", ILoggingEvent::getInstant);
		members.add("log").usingMembers((log) -> {
			log.add("level", ILoggingEvent::getLevel);
			log.add("logger", ILoggingEvent::getLoggerName);
		});
		members.add("process").usingMembers((process) -> {
			process.add("pid", environment.getProperty("spring.application.pid", Long.class)).when(Objects::nonNull);
			process.add("thread").usingMembers((thread) -> thread.add("name", ILoggingEvent::getThreadName));
		});
		ElasticCommonSchemaProperties.get(environment).jsonMembers(members);
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.add().usingPairs(contextPairs.nested((pairs) -> {
			pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap);
			pairs.add(ILoggingEvent::getKeyValuePairs, keyValuePairExtractor);
		}));
		members.add().whenNotNull(ILoggingEvent::getThrowableProxy).usingMembers((throwableMembers) -> {
			throwableMembers.add("error").usingMembers((error) -> {
				error.add("type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
				error.add("message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
				error.add("stack_trace", extractor::stackTrace);
			});
		});
		members.add("tags", ILoggingEvent::getMarkerList)
			.whenNotNull()
			.as(ElasticCommonSchemaStructuredLogFormatter::getMarkers)
			.whenNotEmpty();
		members.add("ecs").usingMembers((ecs) -> ecs.add("version", "8.11"));
	}

	private static Set<String> getMarkers(List<Marker> markers) {
		Set<String> result = new TreeSet<>();
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
