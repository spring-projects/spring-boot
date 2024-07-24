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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ElasticCommonSchemaService;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.util.ObjectUtils;

/**
 * Log4j2 {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#ELASTIC_COMMON_SCHEMA}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticCommonSchemaStructuredLogFormatter extends JsonWriterStructuredLogFormatter<LogEvent> {

	ElasticCommonSchemaStructuredLogFormatter(ApplicationPid pid, ElasticCommonSchemaService service) {
		super((members) -> jsonMembers(pid, service, members));
	}

	private static void jsonMembers(ApplicationPid pid, ElasticCommonSchemaService service,
			JsonWriter.Members<LogEvent> members) {
		members.add("@timestamp", LogEvent::getInstant).as(ElasticCommonSchemaStructuredLogFormatter::asTimestamp);
		members.add("log.level", LogEvent::getLevel).as(Level::name);
		members.add("process.pid", pid).when(ApplicationPid::isAvailable).as(ApplicationPid::toLong);
		members.add("process.thread.name", LogEvent::getThreadName);
		service.jsonMembers(members);
		members.add("log.logger", LogEvent::getLoggerName);
		members.add("message", LogEvent::getMessage).as(Message::getFormattedMessage);
		members.add(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.usingPairs((contextData, pairs) -> contextData.forEach(pairs::accept));
		members.add(LogEvent::getThrownProxy).whenNotNull().usingMembers((thrownProxyMembers) -> {
			thrownProxyMembers.add("error.type", ThrowableProxy::getThrowable)
				.whenNotNull()
				.as(ObjectUtils::nullSafeClassName);
			thrownProxyMembers.add("error.message", ThrowableProxy::getMessage);
			thrownProxyMembers.add("error.stack_trace", ThrowableProxy::getExtendedStackTraceAsString);
		});
		members.add("ecs.version", "8.11");
	}

	private static java.time.Instant asTimestamp(Instant instant) {
		return java.time.Instant.ofEpochMilli(instant.getEpochMillisecond()).plusNanos(instant.getNanoOfMillisecond());
	}

}
