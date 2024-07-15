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

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#ELASTIC_COMMON_SCHEMA}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticCommonSchemaStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
			(pair) -> pair.value);

	private JsonWriter<ILoggingEvent> writer;

	ElasticCommonSchemaStructuredLogFormatter(ApplicationMetadata metadata,
			ThrowableProxyConverter throwableProxyConverter) {
		this.writer = JsonWriter
			.<ILoggingEvent>of((members) -> loggingEventJson(metadata, throwableProxyConverter, members))
			.withNewLineAtEnd();
	}

	private void loggingEventJson(ApplicationMetadata metadata, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("@timestamp", ILoggingEvent::getInstant);
		members.add("log.level", ILoggingEvent::getLevel);
		members.add("process.pid", metadata::pid).whenNotNull();
		members.add("process.thread.name", ILoggingEvent::getThreadName);
		members.add("service.name", metadata::name).whenHasLength();
		members.add("service.version", metadata::version).whenHasLength();
		members.add("service.environment", metadata::environment).whenHasLength();
		members.add("service.node.name", metadata::nodeName).whenHasLength();
		members.add("log.logger", ILoggingEvent::getLoggerName);
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.addMapEntries(ILoggingEvent::getMDCPropertyMap);
		members.add(ILoggingEvent::getKeyValuePairs)
			.whenNotEmpty()
			.usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);
		members.addSelf().whenNotNull(ILoggingEvent::getThrowableProxy).usingMembers((throwableMembers) -> {
			throwableMembers.add("error.type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
			throwableMembers.add("error.message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
			throwableMembers.add("error.stack_trace", (event) -> throwableProxyConverter.convert(event));
		});
		members.add("ecs.version", "8.11");
	}

	@Override
	public String format(ILoggingEvent event) {
		return this.writer.writeToString(event);
	}

}
