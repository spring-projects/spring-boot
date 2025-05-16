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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.json.WritableJson;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
			(pair) -> pair.value);

	private static final Log logger = LogFactory.getLog(GraylogExtendedLogFormatStructuredLogFormatter.class);

	/**
	 * Allowed characters in field names are any word character (letter, number,
	 * underscore), dashes and dots.
	 */
	private static final Pattern FIELD_NAME_VALID_PATTERN = Pattern.compile("^[\\w.\\-]*$");

	/**
	 * Libraries SHOULD not allow to send id as additional field ("_id"). Graylog server
	 * nodes omit this field automatically.
	 */
	private static final Set<String> ADDITIONAL_FIELD_ILLEGAL_KEYS = Set.of("id", "_id");

	GraylogExtendedLogFormatStructuredLogFormatter(Environment environment, StackTracePrinter stackTracePrinter,
			ContextPairs contextPairs, ThrowableProxyConverter throwableProxyConverter,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(environment, stackTracePrinter, contextPairs, throwableProxyConverter, members),
				customizer);
	}

	private static void jsonMembers(Environment environment, StackTracePrinter stackTracePrinter,
			ContextPairs contextPairs, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		Extractor extractor = new Extractor(stackTracePrinter, throwableProxyConverter);
		members.add("version", "1.1");
		members.add("short_message", ILoggingEvent::getFormattedMessage)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::getMessageText);
		members.add("timestamp", ILoggingEvent::getTimeStamp)
			.as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
		members.add("level", LevelToSyslogSeverity::convert);
		members.add("_level_name", ILoggingEvent::getLevel);
		members.add("_process_pid", environment.getProperty("spring.application.pid", Long.class))
			.when(Objects::nonNull);
		members.add("_process_thread_name", ILoggingEvent::getThreadName);
		GraylogExtendedLogFormatProperties.get(environment).jsonMembers(members);
		members.add("_log_logger", ILoggingEvent::getLoggerName);
		members.add().usingPairs(contextPairs.flat(additionalFieldJoiner(), (pairs) -> {
			pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap);
			pairs.add(ILoggingEvent::getKeyValuePairs, keyValuePairExtractor);
		}));
		members.add()
			.whenNotNull(ILoggingEvent::getThrowableProxy)
			.usingMembers((throwableMembers) -> throwableMembers(throwableMembers, extractor));
	}

	private static String getMessageText(String formattedMessage) {
		// Always return text as a blank message will lead to a error as of Graylog v6
		return (!StringUtils.hasText(formattedMessage)) ? "(blank)" : formattedMessage;
	}

	/**
	 * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
	 * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
	 * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
	 * @param timeStamp the timestamp of the log message
	 * @return the timestamp formatted as string with millisecond precision
	 */
	private static WritableJson formatTimeStamp(long timeStamp) {
		return (out) -> out.append(new BigDecimal(timeStamp).movePointLeft(3).toPlainString());
	}

	private static void throwableMembers(Members<ILoggingEvent> members, Extractor extractor) {
		members.add("full_message", extractor::messageAndStackTrace);
		members.add("_error_type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
		members.add("_error_stack_trace", extractor::stackTrace);
		members.add("_error_message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
	}

	private static BinaryOperator<String> additionalFieldJoiner() {
		return (prefix, name) -> {
			name = prefix + name;
			if (!FIELD_NAME_VALID_PATTERN.matcher(name).matches()) {
				logger.warn(LogMessage.format("'%s' is not a valid field name according to GELF standard", name));
				return null;
			}
			if (ADDITIONAL_FIELD_ILLEGAL_KEYS.contains(name)) {
				logger.warn(LogMessage.format("'%s' is an illegal field name according to GELF standard", name));
				return null;
			}
			return (!name.startsWith("_")) ? "_" + name : name;
		};
	}

}
